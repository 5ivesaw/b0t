package dev.fivesaw.sawbot.forge.actuator;

import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.action.ActionValidation;
import dev.fivesaw.sawbot.forge.model.ModelActionEnvelope;
import dev.fivesaw.sawbot.forge.safety.InputRelease;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MathHelper;
import org.apache.logging.log4j.Logger;

/** Client-thread-only legitimate-control actuator with strict deadlines and complete release. */
public final class SafeActionActuator {
    private static final float BUTTON_THRESHOLD = 0.5F;
    private static final float AXIS_THRESHOLD = 0.25F;

    private final Minecraft minecraft;
    private final SawBotStateController state;
    private final EnvironmentGuard environment;
    private final long maximumActionAgeNanos;
    private final long maximumSequenceLag;
    private final Logger logger;
    private ActionCommand activeAction;
    private ActionCommand previousAppliedAction = ActionCommand.zero(0L, 0L, "none/0");
    private int remainingTicks;
    private float remainingYaw;
    private float remainingPitch;
    private long acceptedActions;
    private long rejectedActions;
    private long expiredActions;
    private long activeActionAcceptedNanos;
    private long lastActionRoundTripNanos;
    private String status = "IDLE";
    private String lastReason = "startup";

    public SafeActionActuator(Minecraft minecraft, SawBotStateController state,
                              EnvironmentGuard environment, long maximumActionAgeMillis,
                              long maximumSequenceLag, Logger logger) {
        if (minecraft == null || state == null || environment == null || maximumActionAgeMillis < 50L
            || maximumActionAgeMillis > 2000L || maximumSequenceLag < 0L || maximumSequenceLag > 20L
            || logger == null) throw new IllegalArgumentException("actuator configuration");
        this.minecraft = minecraft;
        this.state = state;
        this.environment = environment;
        this.maximumActionAgeNanos = maximumActionAgeMillis * 1_000_000L;
        this.maximumSequenceLag = maximumSequenceLag;
        this.logger = logger;
    }

    public void tick(ObservationSnapshot latest, ModelActionEnvelope incoming) {
        long now = System.nanoTime();
        if (!state.mayApplyAutonomousActions()) {
            release("disabled/frozen");
            return;
        }
        if (!environment.isAllowed()) {
            state.disableAndRelease("environment blocked");
            clear("BLOCKED", "environment blocked");
            return;
        }
        if (minecraft.currentScreen != null) {
            if (incoming == null || !isSafeGuiToggle(incoming.command())) {
                release("GUI open");
                return;
            }
        }
        if (incoming != null) accept(incoming, latest, now);
        if (activeAction == null) {
            release("waiting for action");
            return;
        }
        if (now - activeActionAcceptedNanos > maximumActionAgeNanos || remainingTicks <= 0) {
            expiredActions++;
            release("action expired");
            return;
        }
        applyCurrentTick();
    }

    private void accept(ModelActionEnvelope envelope, ObservationSnapshot latest, long now) {
        ActionCommand command = envelope.command();
        ActionValidation validation = ActionContextValidator.validate(command, latest, now,
            maximumActionAgeNanos, maximumSequenceLag);
        if (!validation.isValid()) {
            rejectedActions++;
            release("reject: " + validation.reason());
            return;
        }
        if (activeAction != null
            && command.observationSequenceNumber() < activeAction.observationSequenceNumber()) {
            rejectedActions++;
            lastReason = "reject: older than active action";
            return;
        }
        activeAction = command;
        remainingTicks = command.actionDurationTicks();
        remainingYaw = clamp(command.yawDeltaDegrees(), -45F, 45F);
        remainingPitch = clamp(command.pitchDeltaDegrees(), -30F, 30F);
        activeActionAcceptedNanos = now;
        lastActionRoundTripNanos = envelope.roundTripNanos();
        acceptedActions++;
        status = "APPLY";
        lastReason = "OK";
        applyOneShots(command);
    }

    private void applyCurrentTick() {
        ActionCommand command = activeAction;
        GameSettings settings = minecraft.gameSettings;
        set(settings.keyBindForward, command.forward() > AXIS_THRESHOLD);
        set(settings.keyBindBack, command.forward() < -AXIS_THRESHOLD);
        set(settings.keyBindRight, command.strafe() > AXIS_THRESHOLD);
        set(settings.keyBindLeft, command.strafe() < -AXIS_THRESHOLD);
        set(settings.keyBindJump, command.jumpProbability() >= BUTTON_THRESHOLD);
        set(settings.keyBindSprint, command.sprintProbability() >= BUTTON_THRESHOLD);
        set(settings.keyBindSneak, command.sneakProbability() >= BUTTON_THRESHOLD);

        int divisor = Math.max(1, remainingTicks);
        float yawStep = remainingYaw / divisor;
        float pitchStep = remainingPitch / divisor;
        remainingYaw -= yawStep;
        remainingPitch -= pitchStep;
        if (minecraft.thePlayer != null) {
            minecraft.thePlayer.rotationYaw += yawStep;
            minecraft.thePlayer.rotationPitch = MathHelper.clamp_float(
                minecraft.thePlayer.rotationPitch + pitchStep, -90F, 90F);
        }
        previousAppliedAction = command;
        remainingTicks--;
        status = "APPLY";
        if (remainingTicks <= 0) {
            activeAction = null;
            releaseContinuous();
            status = "IDLE";
            lastReason = "duration complete";
        }
    }

    private void applyOneShots(ActionCommand command) {
        GameSettings settings = minecraft.gameSettings;
        if (command.hotbarSlot() >= 0 && minecraft.thePlayer != null) {
            minecraft.thePlayer.inventory.currentItem = command.hotbarSlot();
            if (minecraft.playerController != null) minecraft.playerController.updateController();
        }
        pulse(settings.keyBindAttack, command.attackProbability() >= BUTTON_THRESHOLD);
        pulse(settings.keyBindUseItem, command.useOrPlaceProbability() >= BUTTON_THRESHOLD);
        pulse(settings.keyBindDrop, command.dropProbability() >= BUTTON_THRESHOLD);
        pulse(settings.keyBindInventory, command.inventoryToggleProbability() >= BUTTON_THRESHOLD);
    }

    public void release(String reason) {
        releaseContinuous();
        if (activeAction != null) activeAction = null;
        remainingTicks = 0;
        remainingYaw = 0F;
        remainingPitch = 0F;
        activeActionAcceptedNanos = 0L;
        if (!"disabled/frozen".equals(reason) || !"IDLE".equals(status)) {
            status = "IDLE";
            lastReason = reason == null ? "release" : reason;
        }
    }

    private void clear(String nextStatus, String reason) {
        InputRelease.releaseAll(minecraft);
        activeAction = null;
        remainingTicks = 0;
        remainingYaw = 0F;
        remainingPitch = 0F;
        activeActionAcceptedNanos = 0L;
        status = nextStatus;
        lastReason = reason;
    }

    private void releaseContinuous() {
        InputRelease.releaseAll(minecraft);
    }

    private static boolean isSafeGuiToggle(ActionCommand command) {
        return command != null
            && command.inventoryToggleProbability() >= BUTTON_THRESHOLD
            && Math.abs(command.forward()) < AXIS_THRESHOLD
            && Math.abs(command.strafe()) < AXIS_THRESHOLD
            && Math.abs(command.yawDeltaDegrees()) < 0.001F
            && Math.abs(command.pitchDeltaDegrees()) < 0.001F
            && command.jumpProbability() < BUTTON_THRESHOLD
            && command.sprintProbability() < BUTTON_THRESHOLD
            && command.sneakProbability() < BUTTON_THRESHOLD
            && command.attackProbability() < BUTTON_THRESHOLD
            && command.useOrPlaceProbability() < BUTTON_THRESHOLD
            && command.dropProbability() < BUTTON_THRESHOLD
            && command.hotbarSlot() == ActionCommand.KEEP_CURRENT_HOTBAR_SLOT;
    }

    private static void set(KeyBinding binding, boolean down) {
        if (binding != null) KeyBinding.setKeyBindState(binding.getKeyCode(), down);
    }

    private static void pulse(KeyBinding binding, boolean fire) {
        if (fire && binding != null) KeyBinding.onTick(binding.getKeyCode());
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public ActionCommand previousAppliedAction() { return previousAppliedAction; }
    public ActionCommand activeAction() { return activeAction; }
    public String status() { return status; }
    public String lastReason() { return lastReason; }
    public long acceptedActions() { return acceptedActions; }
    public long rejectedActions() { return rejectedActions; }
    public long expiredActions() { return expiredActions; }
    public int remainingTicks() { return remainingTicks; }
    public long lastActionRoundTripNanos() { return lastActionRoundTripNanos; }
    public long maximumActionAgeNanos() { return maximumActionAgeNanos; }
    public long maximumSequenceLag() { return maximumSequenceLag; }
    public String environmentDescription() { return environment.description(); }
}
