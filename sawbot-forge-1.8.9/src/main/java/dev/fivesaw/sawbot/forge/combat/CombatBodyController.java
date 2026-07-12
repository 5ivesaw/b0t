package dev.fivesaw.sawbot.forge.combat;

import dev.fivesaw.sawbot.common.action.AbortCondition;
import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.action.Skill;
import dev.fivesaw.sawbot.common.action.TacticalObjective;
import dev.fivesaw.sawbot.common.combat.CombatMovementDecision;
import dev.fivesaw.sawbot.common.combat.CombatMovementPlanner;
import dev.fivesaw.sawbot.common.observation.EntityKind;
import dev.fivesaw.sawbot.common.observation.EntityObservation;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.common.observation.TeamRelation;
import dev.fivesaw.sawbot.forge.actuator.EnvironmentGuard;
import dev.fivesaw.sawbot.forge.model.ModelActionEnvelope;
import dev.fivesaw.sawbot.forge.navigation.WorldNavigationGrid;
import dev.fivesaw.sawbot.forge.safety.InputRelease;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.apache.logging.log4j.Logger;

/**
 * Reference-driven local PvP motor body.
 *
 * The learned brain or explicit private test harness selects the target. This body only
 * performs legal visible camera movement, spacing, strafing, edge guarding, attack timing,
 * and complete input release. It never scans for or auto-selects the nearest opponent.
 */
public final class CombatBodyController {
    private static final String BODY_VERSION = "combat-body/0.1";
    private static final float ALIGNMENT_MOVEMENT_YAW = 58F;

    private final Minecraft minecraft;
    private final SawBotStateController state;
    private final EnvironmentGuard environment;
    private final CombatMovementPlanner movementPlanner;
    private final HumanRotationController rotationController =
        new HumanRotationController();
    private final float maximumPursuitDistance;
    private final float attackRange;
    private final float maximumYawDegreesPerTick;
    private final float maximumPitchDegreesPerTick;
    private final Logger logger;

    private WorldNavigationGrid worldGrid;
    private boolean manualIntent;
    private int manualTargetTrackingId = -1;
    private int brainTargetTrackingId = -1;
    private long brainIntentDeadlineNanos;
    private TacticalObjective objective = TacticalObjective.NONE;
    private boolean ownsInputs;
    private long lastAttackTick = -1000000L;
    private int attacks;
    private int edgeGuards;
    private int lostTargets;
    private int rejectedTargets;
    private int targetSwitches;
    private int activeTargetTrackingId = -1;
    private float targetDistance;
    private float yawError;
    private float pitchError;
    private String status = "IDLE";
    private String reason = "startup";
    private String source = "none";
    private String movementMode = "IDLE";
    private ActionCommand previousAppliedAction =
        ActionCommand.zero(0L, 0L, BODY_VERSION);

    public CombatBodyController(Minecraft minecraft, SawBotStateController state,
                                EnvironmentGuard environment,
                                float maximumPursuitDistance, float attackRange,
                                int strafeWindowTicks, int attackCooldownTicks,
                                float maximumYawDegreesPerTick,
                                float maximumPitchDegreesPerTick,
                                Logger logger) {
        if (minecraft == null || state == null || environment == null || logger == null
            || maximumPursuitDistance < 3F || maximumPursuitDistance > 16F
            || attackRange < 2.5F || attackRange > 3.2F
            || maximumYawDegreesPerTick < 4F || maximumYawDegreesPerTick > 75F
            || maximumPitchDegreesPerTick < 3F || maximumPitchDegreesPerTick > 60F) {
            throw new IllegalArgumentException("combat body configuration");
        }
        this.minecraft = minecraft;
        this.state = state;
        this.environment = environment;
        this.movementPlanner = new CombatMovementPlanner(strafeWindowTicks,
            attackCooldownTicks);
        this.maximumPursuitDistance = maximumPursuitDistance;
        this.attackRange = attackRange;
        this.maximumYawDegreesPerTick = maximumYawDegreesPerTick;
        this.maximumPitchDegreesPerTick = maximumPitchDegreesPerTick;
        this.logger = logger;
    }

    /** Accepts only an explicit PVP skill and target chosen by the learned brain. */
    public void observeBrainAction(ModelActionEnvelope envelope) {
        if (envelope == null || envelope.command() == null) return;
        ActionCommand command = envelope.command();
        if (command.selectedSkill() == Skill.PVP
            && command.selectedTargetTrackingId() > 0) {
            if (brainTargetTrackingId != command.selectedTargetTrackingId()) {
                targetSwitches++;
            }
            brainTargetTrackingId = command.selectedTargetTrackingId();
            brainIntentDeadlineNanos = System.nanoTime() + 1_500_000_000L;
            objective = command.tacticalObjective();
        } else {
            brainTargetTrackingId = -1;
            brainIntentDeadlineNanos = 0L;
            if (!manualIntent) objective = TacticalObjective.NONE;
        }
    }

    /** Private/local mechanical harness. It never chooses a target automatically. */
    public boolean toggleManualIntent(int selectedTrackingId) {
        if (manualIntent && manualTargetTrackingId == selectedTrackingId) {
            clearManualIntent();
            return false;
        }
        if (selectedTrackingId <= 0) return false;
        manualIntent = true;
        manualTargetTrackingId = selectedTrackingId;
        objective = TacticalObjective.NONE;
        source = "manual";
        return true;
    }

    public void clearManualIntent() {
        manualIntent = false;
        manualTargetTrackingId = -1;
        if (brainTargetTrackingId <= 0) objective = TacticalObjective.NONE;
        release("manual combat intent cleared");
    }

    public boolean hasIntent() {
        expireBrainIntent();
        return manualIntent || brainTargetTrackingId > 0;
    }

    public boolean shouldOwnCombat() {
        return state.isEnabled() && hasIntent();
    }

    public void tick(long clientTick, ObservationSnapshot latest) {
        expireBrainIntent();
        if (!state.mayApplyAutonomousActions()) {
            release("disabled/frozen");
            return;
        }
        if (!environment.isAllowed()) {
            release("environment blocked");
            state.disableAndRelease("environment blocked");
            status = "BLOCKED";
            return;
        }
        if (minecraft.currentScreen != null || minecraft.thePlayer == null
            || minecraft.theWorld == null) {
            release("GUI/world unavailable");
            status = "PAUSED";
            return;
        }

        int requestedTarget = requestedTargetTrackingId();
        if (requestedTarget <= 0) {
            release("no combat target intent");
            status = "IDLE";
            return;
        }
        source = manualIntent ? "manual" : "brain";
        EntityObservation observation = findObservation(latest, requestedTarget);
        if (observation == null || !observation.loaded()) {
            lostTargets++;
            activeTargetTrackingId = requestedTarget;
            releaseMovement("target observation unavailable");
            status = "WAIT_TARGET";
            reason = "selected target #" + requestedTarget + " not in bounded snapshot";
            previousAppliedAction = bodyAction(latest, 0F, 0F, false, false,
                0F, 0F, requestedTarget);
            return;
        }
        if (observation.kind() != EntityKind.PLAYER
            || observation.teamRelation() == TeamRelation.SELF
            || observation.teamRelation() == TeamRelation.TEAMMATE
            || observation.health() <= 0F) {
            rejectedTargets++;
            activeTargetTrackingId = requestedTarget;
            releaseMovement("target is not a legal PvP opponent");
            status = "REJECTED";
            reason = observation.kind() + "/" + observation.teamRelation();
            previousAppliedAction = bodyAction(latest, 0F, 0F, false, false,
                0F, 0F, requestedTarget);
            return;
        }

        Entity target = findMinecraftEntity(observation.minecraftEntityId());
        if (!(target instanceof EntityLivingBase) || target.isDead) {
            lostTargets++;
            releaseMovement("Minecraft target entity unavailable");
            status = "WAIT_TARGET";
            reason = "mc entity #" + observation.minecraftEntityId() + " unavailable";
            return;
        }

        ensureWorldGrid();
        EntityPlayerSP player = minecraft.thePlayer;
        activeTargetTrackingId = observation.trackingId();
        targetDistance = player.getDistanceToEntity(target);
        if (targetDistance > maximumPursuitDistance) {
            releaseMovement("target outside local combat radius");
            status = "OUT_OF_RANGE";
            reason = one(targetDistance) + "m > " + one(maximumPursuitDistance)
                + "m; brain must navigate closer";
            previousAppliedAction = bodyAction(latest, 0F, 0F, false, false,
                0F, 0F, activeTargetTrackingId);
            return;
        }

        Aim aim = aimAt(player, target);
        HumanRotationController.RotationStep rotation = rotationController.step(
            player.rotationYaw, player.rotationPitch, aim.yaw, aim.pitch,
            maximumYawDegreesPerTick, maximumPitchDegreesPerTick);
        player.rotationYaw = HumanRotationController.wrapDegrees(
            player.rotationYaw + rotation.yawStep());
        player.rotationPitch = clamp(player.rotationPitch + rotation.pitchStep(),
            -90F, 90F);
        yawError = rotation.yawError();
        pitchError = rotation.pitchError();

        boolean lineOfSight = observation.lineOfSight()
            && player.canEntityBeSeen(target);
        boolean forwardSafe = supportProbe(player, 0.90D, 0D);
        boolean backSafe = supportProbe(player, -0.78D, 0D);
        boolean leftSafe = supportProbe(player, 0D, -0.82D);
        boolean rightSafe = supportProbe(player, 0D, 0.82D);
        int targetHurtTicks = Math.max(observation.hurtTimerTicks(),
            ((EntityLivingBase)target).hurtTime);
        long elapsed = Math.max(0L, clientTick - lastAttackTick);
        int ticksSinceAttack = elapsed > Integer.MAX_VALUE
            ? Integer.MAX_VALUE : (int)elapsed;
        boolean currentlyAttackable = lineOfSight
            && targetDistance <= attackRange;
        CombatMovementDecision decision = movementPlanner.plan(clientTick,
            activeTargetTrackingId, targetDistance, yawError, pitchError,
            lineOfSight, currentlyAttackable, targetHurtTicks,
            ticksSinceAttack, forwardSafe, backSafe, leftSafe, rightSafe);

        float forward = decision.forward();
        float strafe = decision.strafe();
        boolean sprint = decision.sprint();
        boolean attack = decision.attack();
        if (Math.abs(yawError) > ALIGNMENT_MOVEMENT_YAW) {
            forward = 0F;
            strafe = 0F;
            sprint = false;
            attack = false;
            movementMode = "ALIGN";
        } else {
            movementMode = decision.mode();
        }
        if (decision.edgeGuarded()) edgeGuards++;

        applyMovement(forward, strafe, sprint);
        boolean attacked = false;
        if (attack && minecraft.playerController != null) {
            minecraft.playerController.attackEntity(player, target);
            player.swingItem();
            lastAttackTick = clientTick;
            attacks++;
            attacked = true;
        }
        status = lineOfSight ? "ENGAGE" : "OCCLUDED";
        reason = movementMode + " dist " + one(targetDistance)
            + " yaw/pitch " + one(yawError) + "/" + one(pitchError);
        previousAppliedAction = bodyAction(latest, forward, strafe, sprint,
            attacked, rotation.yawStep(), rotation.pitchStep(),
            activeTargetTrackingId);
    }

    private void expireBrainIntent() {
        if (brainTargetTrackingId > 0
            && System.nanoTime() > brainIntentDeadlineNanos) {
            brainTargetTrackingId = -1;
            brainIntentDeadlineNanos = 0L;
            if (!manualIntent) objective = TacticalObjective.NONE;
        }
    }

    private int requestedTargetTrackingId() {
        if (manualIntent) return manualTargetTrackingId;
        return brainTargetTrackingId;
    }

    private static EntityObservation findObservation(ObservationSnapshot snapshot,
                                                     int trackingId) {
        if (snapshot == null || trackingId <= 0) return null;
        for (EntityObservation entity : snapshot.entities().entities()) {
            if (entity.trackingId() == trackingId) return entity;
        }
        return null;
    }

    private Entity findMinecraftEntity(int minecraftEntityId) {
        List<?> loaded = minecraft.theWorld.loadedEntityList;
        for (Object raw : loaded) {
            if (raw instanceof Entity
                && ((Entity)raw).getEntityId() == minecraftEntityId) {
                return (Entity)raw;
            }
        }
        return null;
    }

    private void ensureWorldGrid() {
        if (worldGrid == null || !worldGrid.matchesWorld(minecraft.theWorld)) {
            worldGrid = new WorldNavigationGrid(minecraft.theWorld);
        }
    }

    private boolean supportProbe(EntityPlayerSP player, double forward,
                                 double strafe) {
        double radians = Math.toRadians(player.rotationYaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        double rightX = Math.cos(radians);
        double rightZ = Math.sin(radians);
        int x = floor(player.posX + forwardX * forward + rightX * strafe);
        int y = floor(player.posY);
        int z = floor(player.posZ + forwardZ * forward + rightZ * strafe);
        return worldGrid.refreshStandable(x, y, z);
    }

    private static Aim aimAt(EntityPlayerSP player, Entity target) {
        double predictedX = target.posX + target.motionX * 1.35D;
        double predictedZ = target.posZ + target.motionZ * 1.35D;
        double height = target.getEntityBoundingBox().maxY
            - target.getEntityBoundingBox().minY;
        double predictedY = target.getEntityBoundingBox().minY
            + height * 0.72D + target.motionY * 0.65D;
        double dx = predictedX - player.posX;
        double dy = predictedY - (player.posY + player.getEyeHeight());
        double dz = predictedZ - player.posZ;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90D);
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy,
            Math.max(0.0001D, horizontal))));
        return new Aim(HumanRotationController.wrapDegrees(yaw),
            clamp(pitch, -90F, 90F));
    }

    private void applyMovement(float forward, float strafe, boolean sprint) {
        GameSettings settings = minecraft.gameSettings;
        set(settings.keyBindForward, forward > 0.20F);
        set(settings.keyBindBack, forward < -0.20F);
        set(settings.keyBindRight, strafe > 0.20F);
        set(settings.keyBindLeft, strafe < -0.20F);
        set(settings.keyBindSprint, sprint);
        set(settings.keyBindJump, false);
        set(settings.keyBindSneak, false);
        ownsInputs = Math.abs(forward) > 0.20F || Math.abs(strafe) > 0.20F
            || sprint;
    }

    public void releaseIfOwned(String releaseReason) {
        if (ownsInputs) releaseMovement(releaseReason);
    }

    public void release(String releaseReason) {
        releaseMovement(releaseReason);
        status = "IDLE";
        movementMode = "IDLE";
        reason = releaseReason == null ? "release" : releaseReason;
        activeTargetTrackingId = -1;
        targetDistance = 0F;
        yawError = 0F;
        pitchError = 0F;
        previousAppliedAction = ActionCommand.zero(0L, 0L, BODY_VERSION);
    }

    public void onWorldUnavailable() {
        worldGrid = null;
        manualIntent = false;
        manualTargetTrackingId = -1;
        brainTargetTrackingId = -1;
        brainIntentDeadlineNanos = 0L;
        objective = TacticalObjective.NONE;
        source = "none";
        release("world unavailable");
    }

    private void releaseMovement(String releaseReason) {
        GameSettings settings = minecraft.gameSettings;
        InputRelease.restorePhysical(settings.keyBindForward);
        InputRelease.restorePhysical(settings.keyBindBack);
        InputRelease.restorePhysical(settings.keyBindLeft);
        InputRelease.restorePhysical(settings.keyBindRight);
        InputRelease.restorePhysical(settings.keyBindJump);
        InputRelease.restorePhysical(settings.keyBindSneak);
        InputRelease.restorePhysical(settings.keyBindSprint);
        InputRelease.restorePhysical(settings.keyBindAttack);
        ownsInputs = false;
        rotationController.reset();
        reason = releaseReason == null ? "release" : releaseReason;
    }

    private ActionCommand bodyAction(ObservationSnapshot latest, float forward,
                                     float strafe, boolean sprint,
                                     boolean attack, float yawDelta,
                                     float pitchDelta, int targetTrackingId) {
        long sequence = latest == null ? 0L : latest.sequenceNumber();
        long timestamp = latest == null ? 0L : latest.monotonicTimestampNanos();
        return new ActionCommand(sequence, timestamp, BODY_VERSION,
            forward, strafe, yawDelta, pitchDelta,
            0F, sprint ? 1F : 0F, 0F, attack ? 1F : 0F,
            0F, 0F, 0F, ActionCommand.KEEP_CURRENT_HOTBAR_SLOT,
            Skill.PVP, targetTrackingId, -1, 1F, 1,
            objective, AbortCondition.NONE);
    }

    private static void set(KeyBinding binding, boolean down) {
        if (binding != null) KeyBinding.setKeyBindState(binding.getKeyCode(), down);
    }

    private static int floor(double value) {
        int integer = (int)value;
        return value < integer ? integer - 1 : integer;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static String one(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    public ActionCommand previousAppliedAction() { return previousAppliedAction; }
    public boolean ownsInputs() { return ownsInputs; }
    public String status() { return status; }
    public String reason() { return reason; }
    public String source() { return source; }
    public String movementMode() { return movementMode; }
    public int activeTargetTrackingId() { return activeTargetTrackingId; }
    public float targetDistance() { return targetDistance; }
    public float yawError() { return yawError; }
    public float pitchError() { return pitchError; }
    public int attacks() { return attacks; }
    public int edgeGuards() { return edgeGuards; }
    public int lostTargets() { return lostTargets; }
    public int rejectedTargets() { return rejectedTargets; }
    public int targetSwitches() { return targetSwitches; }
    public int manualTargetTrackingId() { return manualTargetTrackingId; }
    public int brainTargetTrackingId() { return brainTargetTrackingId; }

    private static final class Aim {
        final float yaw;
        final float pitch;
        Aim(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }
}
