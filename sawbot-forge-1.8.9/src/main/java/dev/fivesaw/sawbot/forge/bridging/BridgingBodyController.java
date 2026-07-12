package dev.fivesaw.sawbot.forge.bridging;

import dev.fivesaw.sawbot.common.action.AbortCondition;
import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.action.Skill;
import dev.fivesaw.sawbot.common.action.TacticalObjective;
import dev.fivesaw.sawbot.common.bridging.BridgeCorridorPlan;
import dev.fivesaw.sawbot.common.bridging.BridgeCorridorPlanner;
import dev.fivesaw.sawbot.common.bridging.BridgeDirection;
import dev.fivesaw.sawbot.common.bridging.BridgePlacementStep;
import dev.fivesaw.sawbot.common.navigation.NavigationCell;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.forge.actuator.EnvironmentGuard;
import dev.fivesaw.sawbot.forge.map.NavigationWaypointController;
import dev.fivesaw.sawbot.forge.model.ModelActionEnvelope;
import dev.fivesaw.sawbot.forge.safety.InputRelease;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.apache.logging.log4j.Logger;

/**
 * Deterministic specialist for legal, visible short-gap bridging.
 *
 * The learned brain selects BRIDGING and the target waypoint. This body owns only
 * mechanics: bounded corridor generation, block-slot selection, visible camera
 * alignment, reach/line-of-sight validation, one placement attempt at a time,
 * server/world confirmation, cautious movement, replanning, and complete release.
 */
public final class BridgingBodyController {
    private static final String BODY_VERSION = "bridging-body/0.1";
    private static final long BRAIN_INTENT_NANOS = 1_500_000_000L;
    private static final double NEXT_CELL_REACHED_SQUARED = 0.46D * 0.46D;
    private static final double MAX_REACH_SQUARED = 4.45D * 4.45D;
    private static final float YAW_TOLERANCE = 2.2F;
    private static final float PITCH_TOLERANCE = 2.2F;
    private static final int COMPLETE_HOLD_TICKS = 8;

    private final Minecraft minecraft;
    private final SawBotStateController state;
    private final EnvironmentGuard environment;
    private final NavigationWaypointController waypoint;
    private final BridgeCorridorPlanner corridorPlanner = new BridgeCorridorPlanner();
    private final int maximumBridgeSteps;
    private final int placementConfirmationTicks;
    private final int maximumPlacementAttempts;
    private final int replanIntervalTicks;
    private final float maximumYawDegreesPerTick;
    private final float maximumPitchDegreesPerTick;
    private final Logger logger;

    private BridgeCorridorPlan plan;
    private int stepIndex;
    private long plannedWaypointRevision = -1L;
    private long lastPlanTick = Long.MIN_VALUE;
    private boolean manualIntent;
    private boolean brainIntent;
    private long brainIntentDeadlineNanos;
    private boolean ownsInputs;
    private int originalHotbarSlot = -1;
    private int selectedBlockSlot = -1;
    private BlockPos targetSupport;
    private BlockPos lastConfirmedSupport;
    private BlockPos pendingPlacementSupport;
    private BlockPos attachmentSupport;
    private EnumFacing attachmentFace;
    private Vec3 attachmentHitVec;
    private int confirmationTicks;
    private int placementAttempts;
    private int placedBlocks;
    private int failedPlacements;
    private int replans;
    private int retargets;
    private int completeTicks;
    private int consecutiveBlockedTicks;
    private String status = "IDLE";
    private String reason = "startup";
    private String source = "automatic";
    private ActionCommand previousAppliedAction = ActionCommand.zero(0L, 0L, BODY_VERSION);

    public BridgingBodyController(Minecraft minecraft, SawBotStateController state,
                                  EnvironmentGuard environment,
                                  NavigationWaypointController waypoint,
                                  int maximumBridgeSteps,
                                  int placementConfirmationTicks,
                                  int maximumPlacementAttempts,
                                  int replanIntervalTicks,
                                  float maximumYawDegreesPerTick,
                                  float maximumPitchDegreesPerTick,
                                  Logger logger) {
        if (minecraft == null || state == null || environment == null || waypoint == null || logger == null) {
            throw new IllegalArgumentException("bridging body component");
        }
        if (maximumBridgeSteps < 1 || placementConfirmationTicks < 2
            || maximumPlacementAttempts < 1 || replanIntervalTicks < 1
            || maximumYawDegreesPerTick < 1F || maximumPitchDegreesPerTick < 1F) {
            throw new IllegalArgumentException("bridging body configuration");
        }
        this.minecraft = minecraft;
        this.state = state;
        this.environment = environment;
        this.waypoint = waypoint;
        this.maximumBridgeSteps = maximumBridgeSteps;
        this.placementConfirmationTicks = placementConfirmationTicks;
        this.maximumPlacementAttempts = maximumPlacementAttempts;
        this.replanIntervalTicks = replanIntervalTicks;
        this.maximumYawDegreesPerTick = maximumYawDegreesPerTick;
        this.maximumPitchDegreesPerTick = maximumPitchDegreesPerTick;
        this.logger = logger;
    }

    public void observeBrainAction(ModelActionEnvelope envelope) {
        if (envelope == null || envelope.command() == null) return;
        ActionCommand command = envelope.command();
        if (command.selectedSkill() == Skill.BRIDGING
            && command.selectedWaypointId() == NavigationWaypointController.USER_WAYPOINT_ID) {
            brainIntent = true;
            brainIntentDeadlineNanos = System.nanoTime() + BRAIN_INTENT_NANOS;
            source = "brain";
        }
    }

    public boolean toggleManualIntent() {
        manualIntent = !manualIntent;
        source = manualIntent ? "manual" : "automatic";
        if (!manualIntent) {
            release("manual bridge intent off");
            clearPlan("manual bridge intent off");
        }
        return manualIntent;
    }

    public void clearManualIntent() {
        manualIntent = false;
        if (!brainIntent) source = "automatic";
        release("manual bridge intent cleared");
        clearPlan("manual bridge intent cleared");
    }

    /**
     * Automatic handoff is allowed only after normal navigation reports a route
     * failure/block and this controller can identify at least one missing support
     * cell on a bounded corridor toward the same waypoint.
     */
    public boolean shouldOwnBridge(String navigationStatus) {
        if (!waypoint.active() || minecraft.thePlayer == null || minecraft.theWorld == null) return false;
        if (plannedWaypointRevision != waypoint.revision()) {
            completeTicks = 0;
        }
        if (brainIntent && System.nanoTime() > brainIntentDeadlineNanos) {
            brainIntent = false;
            if (!manualIntent) source = "automatic";
        }
        boolean explicit = manualIntent || brainIntent;
        boolean automatic = "NO_PATH".equals(navigationStatus) || "BLOCKED".equals(navigationStatus);
        if (!explicit && !automatic) return false;
        if (completeTicks > 0) return false;
        return corridorNeedsPlacement();
    }

    public void tick(long clientTick, ObservationSnapshot latest) {
        if (!state.mayApplyAutonomousActions()) {
            releaseOwnedInputs("disabled/frozen");
            status = "IDLE";
            return;
        }
        if (!environment.isAllowed()) {
            release("environment blocked");
            state.disableAndRelease("environment blocked");
            status = "BLOCKED";
            return;
        }
        if (!waypoint.active()) {
            release("no waypoint");
            clearPlan("waiting for waypoint");
            status = "WAITING";
            return;
        }
        if (minecraft.currentScreen != null || minecraft.thePlayer == null || minecraft.theWorld == null) {
            releaseOwnedInputs("GUI/world unavailable");
            status = "PAUSED";
            reason = minecraft.currentScreen != null ? "GUI open" : "world unavailable";
            return;
        }

        EntityPlayerSP player = minecraft.thePlayer;
        WorldClient world = (WorldClient)minecraft.theWorld;
        confirmPendingPlacement(world);
        if (completeTicks > 0) {
            completeTicks--;
            releaseOwnedInputs("bridge complete");
            status = "COMPLETE";
            previousAppliedAction = bodyAction(latest, false, false, false, false, 0F, 0F);
            return;
        }

        if (plan == null || plannedWaypointRevision != waypoint.revision()
            || clientTick - lastPlanTick >= replanIntervalTicks) {
            buildPlan(clientTick, player, plannedWaypointRevision != waypoint.revision()
                ? "waypoint changed" : "rolling bridge replan");
        }

        BridgePlacementStep step = nextRelevantStep(player, world);
        if (step == null) {
            completeTicks = COMPLETE_HOLD_TICKS;
            status = "COMPLETE";
            reason = "corridor supported";
            releaseOwnedInputs(reason);
            state.setInspectorNotice("BRIDGE COMPLETE: navigation may resume", 1);
            previousAppliedAction = bodyAction(latest, false, false, false, false, 0F, 0F);
            return;
        }

        targetSupport = blockPos(step.supportCell());
        if (isSolidSupport(world, targetSupport)) {
            if (placementAttempts > 0 || confirmationTicks > 0) {
                confirmPlacedBlock(step, latest);
            }
            moveOntoSupportedCell(player, step, latest);
            return;
        }

        if (!isReplaceable(world, targetSupport)) {
            failStep("target support is occupied", true);
            previousAppliedAction = bodyAction(latest, false, false, false, true, 0F, 0F);
            return;
        }
        if (!feetSpaceClear(world, step.feetCell())) {
            failStep("feet/head path blocked", true);
            previousAppliedAction = bodyAction(latest, false, false, false, true, 0F, 0F);
            return;
        }

        int blockSlot = findBridgeBlockSlot(player);
        if (blockSlot < 0) {
            status = "OUT_OF_BLOCKS";
            reason = "no full solid ItemBlock in hotbar";
            releaseOwnedInputs(reason);
            previousAppliedAction = bodyAction(latest, false, false, false, true, 0F, 0F);
            return;
        }
        selectHotbarSlot(player, blockSlot);

        PlacementTarget placement = findPlacementTarget(world, targetSupport, step.direction());
        if (placement == null) {
            failStep("no adjacent legal support face", false);
            previousAppliedAction = bodyAction(latest, false, false, false, true, 0F, 0F);
            return;
        }
        attachmentSupport = placement.support;
        attachmentFace = placement.face;
        attachmentHitVec = placement.hitVec;

        holdSafePlacementInputs();
        float[] desired = rotationTo(player, attachmentHitVec);
        float yawApplied = turnYaw(player, desired[0]);
        float pitchApplied = turnPitch(player, desired[1]);
        status = "ALIGN";
        reason = "step " + (stepIndex + 1) + "/" + plan.size()
            + " face " + attachmentFace;

        if (Math.abs(wrapDegrees(desired[0] - player.rotationYaw)) <= YAW_TOLERANCE
            && Math.abs(desired[1] - player.rotationPitch) <= PITCH_TOLERANCE) {
            if (isSolidSupport(world, targetSupport)) {
                confirmPlacedBlock(step, latest);
                moveOntoSupportedCell(player, step, latest);
                return;
            }
            if (!lineOfSightMatches(player, world, placement)) {
                status = "AIM_BLOCKED";
                reason = "ray trace does not match support face";
                consecutiveBlockedTicks++;
                if (consecutiveBlockedTicks >= placementConfirmationTicks) {
                    failStep(reason, false);
                }
            } else if (confirmationTicks > 0) {
                confirmationTicks--;
                status = "CONFIRM";
                reason = "waiting for placed block " + confirmationTicks + "t";
                if (confirmationTicks == 0) {
                    if (isSolidSupport(world, targetSupport)) {
                        confirmPlacedBlock(step, latest);
                        moveOntoSupportedCell(player, step, latest);
                        return;
                    }
                    if (placementAttempts >= maximumPlacementAttempts) {
                        failedPlacements++;
                        failStep("placement confirmation timeout", true);
                    }
                }
            } else if (placementAttempts < maximumPlacementAttempts) {
                attemptPlacement(player, world, placement);
                placementAttempts++;
                confirmationTicks = placementConfirmationTicks;
                status = "PLACE";
                reason = "attempt " + placementAttempts + "/" + maximumPlacementAttempts;
                consecutiveBlockedTicks = 0;
            }
        }

        previousAppliedAction = bodyAction(latest, false, false, false, true,
            yawApplied, pitchApplied);
    }

    private void buildPlan(long clientTick, EntityPlayerSP player, String why) {
        NavigationCell start = currentFeetCell(player);
        NavigationCell goal = new NavigationCell(floor(waypoint.x()), start.y(), floor(waypoint.z()));
        plan = corridorPlanner.plan(start, goal, maximumBridgeSteps);
        stepIndex = 0;
        plannedWaypointRevision = waypoint.revision();
        lastPlanTick = clientTick;
        targetSupport = null;
        lastConfirmedSupport = null;
        attachmentSupport = null;
        attachmentFace = null;
        attachmentHitVec = null;
        confirmationTicks = 0;
        placementAttempts = 0;
        consecutiveBlockedTicks = 0;
        replans++;
        reason = why + "; " + plan.reason();
        status = plan.size() == 0 ? "NO_CORRIDOR" : "PLAN";
    }

    private boolean corridorNeedsPlacement() {
        EntityPlayerSP player = minecraft.thePlayer;
        World world = minecraft.theWorld;
        if (player == null || world == null) return false;
        NavigationCell start = currentFeetCell(player);
        NavigationCell goal = new NavigationCell(floor(waypoint.x()), start.y(), floor(waypoint.z()));
        BridgeCorridorPlan probe = corridorPlanner.plan(start, goal, maximumBridgeSteps);
        for (BridgePlacementStep step : probe.steps()) {
            BlockPos support = blockPos(step.supportCell());
            if (!isSolidSupport(world, support) && isReplaceable(world, support)
                && feetSpaceClear(world, step.feetCell())) return true;
            if (!feetSpaceClear(world, step.feetCell())) return false;
        }
        return false;
    }

    private BridgePlacementStep nextRelevantStep(EntityPlayerSP player, World world) {
        if (plan == null || plan.size() == 0) return null;
        int nearest = nearestStepIndex(player);
        if (nearest > stepIndex) {
            stepIndex = nearest;
            retargets++;
            resetPlacementAttempt();
        }
        while (stepIndex < plan.size()) {
            BridgePlacementStep candidate = plan.step(stepIndex);
            double distance = horizontalDistanceSquared(player, candidate.feetCell());
            if (isSolidSupport(world, blockPos(candidate.supportCell()))
                && distance <= NEXT_CELL_REACHED_SQUARED) {
                stepIndex++;
                resetPlacementAttempt();
                continue;
            }
            return candidate;
        }
        return null;
    }

    private int nearestStepIndex(EntityPlayerSP player) {
        if (plan == null || plan.size() == 0) return 0;
        int best = Math.max(0, Math.min(stepIndex, plan.size() - 1));
        double bestDistance = horizontalDistanceSquared(player, plan.step(best).feetCell());
        int end = Math.min(plan.size() - 1, best + 4);
        for (int index = best + 1; index <= end; index++) {
            double distance = horizontalDistanceSquared(player, plan.step(index).feetCell());
            if (distance + 0.05D < bestDistance) {
                best = index;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void moveOntoSupportedCell(EntityPlayerSP player, BridgePlacementStep step,
                                       ObservationSnapshot latest) {
        double targetX = step.feetCell().centerX();
        double targetZ = step.feetCell().centerZ();
        float desiredYaw = yawTo(player.posX, player.posZ, targetX, targetZ);
        float yawApplied = turnYaw(player, desiredYaw);
        float pitchApplied = turnPitch(player, 18F);
        double distance = horizontalDistanceSquared(player, step.feetCell());
        boolean forward = distance > NEXT_CELL_REACHED_SQUARED;
        GameSettings settings = minecraft.gameSettings;
        if (settings != null) {
            ownsInputs = true;
            set(settings.keyBindForward, forward);
            set(settings.keyBindBack, false);
            set(settings.keyBindLeft, false);
            set(settings.keyBindRight, false);
            set(settings.keyBindJump, false);
            set(settings.keyBindSprint, false);
            set(settings.keyBindSneak, true);
            set(settings.keyBindUseItem, false);
        }
        status = forward ? "ADVANCE" : "CONFIRMED";
        reason = "support ready at step " + (stepIndex + 1) + "/" + plan.size();
        if (!forward) {
            stepIndex++;
            resetPlacementAttempt();
        }
        previousAppliedAction = bodyAction(latest, forward, false, false, true,
            yawApplied, pitchApplied);
    }

    private void attemptPlacement(EntityPlayerSP player, WorldClient world, PlacementTarget placement) {
        ItemStack stack = player.inventory.mainInventory[selectedBlockSlot];
        if (stack == null || stack.stackSize <= 0) {
            status = "OUT_OF_BLOCKS";
            reason = "selected block stack empty";
            return;
        }
        boolean accepted = minecraft.playerController.onPlayerRightClick(player, world, stack,
            placement.support, placement.face, placement.hitVec);
        if (accepted) {
            pendingPlacementSupport = targetSupport;
            player.swingItem();
        }
        minecraft.playerController.updateController();
    }

    private void confirmPlacedBlock(BridgePlacementStep step, ObservationSnapshot latest) {
        confirmationTicks = 0;
        placementAttempts = 0;
        consecutiveBlockedTicks = 0;
        status = "CONFIRMED";
        reason = "support confirmed at " + step.supportCell();
        previousAppliedAction = bodyAction(latest, false, false, true, true, 0F, 0F);
    }

    private void confirmPendingPlacement(World world) {
        if (pendingPlacementSupport == null || !isSolidSupport(world, pendingPlacementSupport)) return;
        if (!pendingPlacementSupport.equals(lastConfirmedSupport)) {
            placedBlocks++;
            lastConfirmedSupport = pendingPlacementSupport;
            state.setInspectorNotice("BRIDGE PLACE " + placedBlocks + ": "
                + pendingPlacementSupport.getX() + "," + pendingPlacementSupport.getY() + ","
                + pendingPlacementSupport.getZ(), 1);
        }
        pendingPlacementSupport = null;
        confirmationTicks = 0;
        placementAttempts = 0;
        consecutiveBlockedTicks = 0;
    }

    private void failStep(String why, boolean forceReplan) {
        releaseOwnedInputs(why);
        status = "BLOCKED";
        reason = why;
        if (forceReplan) {
            lastPlanTick = Long.MIN_VALUE;
            resetPlacementAttempt();
        }
    }

    private PlacementTarget findPlacementTarget(World world, BlockPos target,
                                                 BridgeDirection preferredDirection) {
        EnumFacing preferred = facingFromDirection(preferredDirection);
        EnumFacing[] order = {
            preferred, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.DOWN
        };
        for (EnumFacing faceFromSupport : order) {
            BlockPos support = new BlockPos(
                target.getX() - faceFromSupport.getFrontOffsetX(),
                target.getY() - faceFromSupport.getFrontOffsetY(),
                target.getZ() - faceFromSupport.getFrontOffsetZ());
            if (!isSolidSupport(world, support)) continue;
            Vec3 hit = faceCenter(support, faceFromSupport);
            return new PlacementTarget(support, faceFromSupport, hit);
        }
        return null;
    }

    private boolean lineOfSightMatches(EntityPlayerSP player, World world,
                                       PlacementTarget placement) {
        Vec3 eyes = player.getPositionEyes(1F);
        double dx = placement.hitVec.xCoord - eyes.xCoord;
        double dy = placement.hitVec.yCoord - eyes.yCoord;
        double dz = placement.hitVec.zCoord - eyes.zCoord;
        if (dx * dx + dy * dy + dz * dz > MAX_REACH_SQUARED) return false;
        MovingObjectPosition hit = world.rayTraceBlocks(eyes, placement.hitVec,
            false, true, false);
        return hit != null
            && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            && placement.support.equals(hit.getBlockPos())
            && (hit.sideHit == null || hit.sideHit == placement.face);
    }

    private int findBridgeBlockSlot(EntityPlayerSP player) {
        int best = -1;
        int bestCount = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.inventory.mainInventory[slot];
            if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemBlock)) continue;
            Block block = ((ItemBlock)stack.getItem()).getBlock();
            if (block == null || !block.isFullBlock() || !block.getMaterial().blocksMovement()) continue;
            if (stack.stackSize > bestCount) {
                best = slot;
                bestCount = stack.stackSize;
            }
        }
        return best;
    }

    private void selectHotbarSlot(EntityPlayerSP player, int slot) {
        if (selectedBlockSlot == slot && player.inventory.currentItem == slot) return;
        if (originalHotbarSlot < 0) originalHotbarSlot = player.inventory.currentItem;
        selectedBlockSlot = slot;
        player.inventory.currentItem = slot;
        minecraft.playerController.updateController();
    }

    private void holdSafePlacementInputs() {
        GameSettings settings = minecraft.gameSettings;
        if (settings == null) return;
        ownsInputs = true;
        set(settings.keyBindForward, false);
        set(settings.keyBindBack, false);
        set(settings.keyBindLeft, false);
        set(settings.keyBindRight, false);
        set(settings.keyBindJump, false);
        set(settings.keyBindSprint, false);
        set(settings.keyBindSneak, true);
        set(settings.keyBindUseItem, false);
    }

    public void release(String why) {
        releaseOwnedInputs(why);
        resetPlacementAttempt();
        completeTicks = 0;
        pendingPlacementSupport = null;
    }

    private void releaseOwnedInputs(String why) {
        if (minecraft.gameSettings != null && ownsInputs) {
            GameSettings settings = minecraft.gameSettings;
            InputRelease.restorePhysical(settings.keyBindForward);
            InputRelease.restorePhysical(settings.keyBindBack);
            InputRelease.restorePhysical(settings.keyBindLeft);
            InputRelease.restorePhysical(settings.keyBindRight);
            InputRelease.restorePhysical(settings.keyBindJump);
            InputRelease.restorePhysical(settings.keyBindSprint);
            InputRelease.restorePhysical(settings.keyBindSneak);
            InputRelease.restorePhysical(settings.keyBindUseItem);
        }
        ownsInputs = false;
        restoreOriginalSlot();
        reason = why == null ? "released" : why;
    }

    public void releaseIfOwned(String why) {
        if (ownsInputs) releaseOwnedInputs(why);
    }

    public void onWorldUnavailable() {
        release("world unavailable");
        clearPlan("world unavailable");
        manualIntent = false;
        brainIntent = false;
        source = "automatic";
    }

    private void restoreOriginalSlot() {
        if (originalHotbarSlot >= 0 && minecraft.thePlayer != null) {
            minecraft.thePlayer.inventory.currentItem = originalHotbarSlot;
            minecraft.playerController.updateController();
        }
        originalHotbarSlot = -1;
        selectedBlockSlot = -1;
    }

    private void clearPlan(String why) {
        plan = null;
        stepIndex = 0;
        plannedWaypointRevision = -1L;
        lastPlanTick = Long.MIN_VALUE;
        targetSupport = null;
        attachmentSupport = null;
        attachmentFace = null;
        attachmentHitVec = null;
        completeTicks = 0;
        resetPlacementAttempt();
        reason = why;
        status = "IDLE";
    }

    private void resetPlacementAttempt() {
        confirmationTicks = 0;
        placementAttempts = 0;
        consecutiveBlockedTicks = 0;
        targetSupport = null;
        attachmentSupport = null;
        attachmentFace = null;
        attachmentHitVec = null;
    }

    private static boolean feetSpaceClear(World world, NavigationCell feet) {
        BlockPos feetPos = new BlockPos(feet.x(), feet.y(), feet.z());
        BlockPos headPos = new BlockPos(feet.x(), feet.y() + 1, feet.z());
        return isReplaceable(world, feetPos) && isReplaceable(world, headPos);
    }

    private static boolean isReplaceable(World world, BlockPos position) {
        if (!world.isBlockLoaded(position)) return false;
        IBlockState state = world.getBlockState(position);
        return state.getBlock().isReplaceable(world, position)
            || !state.getBlock().getMaterial().blocksMovement();
    }

    private static boolean isSolidSupport(World world, BlockPos position) {
        if (!world.isBlockLoaded(position)) return false;
        IBlockState state = world.getBlockState(position);
        Block block = state.getBlock();
        return block.getMaterial().blocksMovement() && !block.isReplaceable(world, position);
    }

    private static NavigationCell currentFeetCell(EntityPlayerSP player) {
        return new NavigationCell(floor(player.posX), floor(player.posY + 0.01D), floor(player.posZ));
    }

    private static BlockPos blockPos(NavigationCell cell) {
        return new BlockPos(cell.x(), cell.y(), cell.z());
    }

    private static EnumFacing facingFromDirection(BridgeDirection direction) {
        switch (direction) {
            case NORTH: return EnumFacing.NORTH;
            case SOUTH: return EnumFacing.SOUTH;
            case WEST: return EnumFacing.WEST;
            case EAST: return EnumFacing.EAST;
            default: throw new IllegalArgumentException("direction");
        }
    }

    private static Vec3 faceCenter(BlockPos support, EnumFacing face) {
        return new Vec3(
            support.getX() + 0.5D + face.getFrontOffsetX() * 0.499D,
            support.getY() + 0.5D + face.getFrontOffsetY() * 0.499D,
            support.getZ() + 0.5D + face.getFrontOffsetZ() * 0.499D);
    }

    private float turnYaw(EntityPlayerSP player, float desiredYaw) {
        float delta = wrapDegrees(desiredYaw - player.rotationYaw);
        float applied = clamp(delta, -maximumYawDegreesPerTick, maximumYawDegreesPerTick);
        player.rotationYaw = wrapDegrees(player.rotationYaw + applied);
        return applied;
    }

    private float turnPitch(EntityPlayerSP player, float desiredPitch) {
        float clampedDesired = clamp(desiredPitch, -90F, 90F);
        float delta = clampedDesired - player.rotationPitch;
        float applied = clamp(delta, -maximumPitchDegreesPerTick, maximumPitchDegreesPerTick);
        player.rotationPitch = clamp(player.rotationPitch + applied, -90F, 90F);
        return applied;
    }

    private static float[] rotationTo(EntityPlayerSP player, Vec3 target) {
        double dx = target.xCoord - player.posX;
        double dy = target.yCoord - (player.posY + player.getEyeHeight());
        double dz = target.zCoord - player.posZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)(Math.atan2(dz, dx) * 180D / Math.PI) - 90F;
        float pitch = (float)(-(Math.atan2(dy, horizontal) * 180D / Math.PI));
        return new float[]{wrapDegrees(yaw), clamp(pitch, -90F, 90F)};
    }

    private static float yawTo(double fromX, double fromZ, double toX, double toZ) {
        return wrapDegrees((float)(Math.atan2(toZ - fromZ, toX - fromX) * 180D / Math.PI) - 90F);
    }

    private ActionCommand bodyAction(ObservationSnapshot latest, boolean forward,
                                     boolean jump, boolean use, boolean sneak,
                                     float yawDelta, float pitchDelta) {
        long sequence = latest == null ? 0L : latest.sequenceNumber();
        return new ActionCommand(sequence, System.nanoTime(), BODY_VERSION,
            forward ? 1F : 0F, 0F, yawDelta, pitchDelta,
            jump ? 1F : 0F, 0F, sneak ? 1F : 0F,
            0F, use ? 1F : 0F, 0F, 0F,
            selectedBlockSlot < 0 ? ActionCommand.KEEP_CURRENT_HOTBAR_SLOT : selectedBlockSlot,
            Skill.BRIDGING, -1, NavigationWaypointController.USER_WAYPOINT_ID,
            1F, 1, TacticalObjective.CONTINUE_CURRENT_OBJECTIVE, AbortCondition.NONE);
    }

    private static void set(KeyBinding binding, boolean down) {
        if (binding != null) KeyBinding.setKeyBindState(binding.getKeyCode(), down);
    }

    private static double horizontalDistanceSquared(EntityPlayerSP player, NavigationCell cell) {
        double dx = cell.centerX() - player.posX;
        double dz = cell.centerZ() - player.posZ;
        return dx * dx + dz * dz;
    }

    private static int floor(double value) {
        int integer = (int)value;
        return value < integer ? integer - 1 : integer;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float wrapDegrees(float value) {
        return MathHelper.wrapAngleTo180_float(value);
    }

    public boolean manualIntent() { return manualIntent; }
    public boolean brainIntent() { return brainIntent; }
    public boolean ownsInputs() { return ownsInputs; }
    public String status() { return status; }
    public String reason() { return reason; }
    public String source() { return source; }
    public int stepIndex() { return stepIndex; }
    public int planSize() { return plan == null ? 0 : plan.size(); }
    public List<BridgePlacementStep> planSteps() {
        return plan == null ? Collections.<BridgePlacementStep>emptyList() : plan.steps();
    }
    public BlockPos targetSupport() { return targetSupport; }
    public BlockPos attachmentSupport() { return attachmentSupport; }
    public EnumFacing attachmentFace() { return attachmentFace; }
    public int selectedBlockSlot() { return selectedBlockSlot; }
    public int placedBlocks() { return placedBlocks; }
    public int failedPlacements() { return failedPlacements; }
    public int replans() { return replans; }
    public int retargets() { return retargets; }
    public int placementAttempts() { return placementAttempts; }
    public int confirmationTicks() { return confirmationTicks; }
    public ActionCommand previousAppliedAction() { return previousAppliedAction; }

    private static final class PlacementTarget {
        final BlockPos support;
        final EnumFacing face;
        final Vec3 hitVec;

        PlacementTarget(BlockPos support, EnumFacing face, Vec3 hitVec) {
            this.support = support;
            this.face = face;
            this.hitVec = hitVec;
        }
    }
}
