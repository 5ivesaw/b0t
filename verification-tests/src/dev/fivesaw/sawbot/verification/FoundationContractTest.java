package dev.fivesaw.sawbot.verification;

import dev.fivesaw.sawbot.common.action.*;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.common.versioning.SchemaVersion;
import dev.fivesaw.sawbot.forge.performance.RollingTimingWindow;
import dev.fivesaw.sawbot.forge.safety.SawBotMode;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.apache.logging.log4j.Logger;

public final class FoundationContractTest {
    public static void main(String[] args) {
        schemaIdentifiersAreStable();
        actionValidationAcceptsCanonicalZero();
        actionValidationRejectsStaleAndNonFiniteCommands();
        actionValidationRejectsOutOfRangeCommands();
        snapshotDefensivelyCopiesArrays();
        snapshotRejectsMissingPreviousAction();
        rollingWindowRemainsBounded();
        safetyControllerReleasesEveryHeldControl();
        System.out.println("PASS FoundationContractTest (8 checks)");
    }

    private static void schemaIdentifiersAreStable() {
        require("sawbot.observation/0.1".equals(SchemaVersion.OBSERVATION_V0_1.identifier()), "observation schema");
        require("sawbot.action/0.1".equals(SchemaVersion.ACTION_V0_1.identifier()), "action schema");
    }

    private static void actionValidationAcceptsCanonicalZero() {
        long now = 1_000_000_000L;
        ActionCommand valid = ActionCommand.zero(10L, now - 10_000_000L, "dummy/0");
        require(ActionValidator.validate(valid, 10L, now, ActionValidator.DEFAULT_MAX_AGE_NANOS, 3L).isValid(), "valid zero action");
    }

    private static void actionValidationRejectsStaleAndNonFiniteCommands() {
        long now = 1_000_000_000L;
        ActionCommand valid = ActionCommand.zero(10L, now - 10_000_000L, "dummy/0");
        require(!ActionValidator.validate(valid, 14L, now, ActionValidator.DEFAULT_MAX_AGE_NANOS, 3L).isValid(), "stale sequence");
        ActionCommand bad = new ActionCommand(10L, now, "dummy/0", Float.NaN, 0f, 0f, 0f,
            0f,0f,0f,0f,0f,0f,0f,-1,Skill.NONE,-1,-1,1f,1,TacticalObjective.NONE,AbortCondition.NONE);
        require(!ActionValidator.validate(bad, 10L, now, ActionValidator.DEFAULT_MAX_AGE_NANOS, 3L).isValid(), "NaN rejected");
    }

    private static void actionValidationRejectsOutOfRangeCommands() {
        long now = 2_000_000_000L;
        ActionCommand bad = new ActionCommand(20L, now, "dummy/0", 1.01f, 0f, 0f, 0f,
            0f,0f,0f,0f,0f,0f,0f,9,Skill.NONE,-1,-1,1f,5,TacticalObjective.NONE,AbortCondition.NONE);
        require(!ActionValidator.validate(bad, 20L, now, ActionValidator.DEFAULT_MAX_AGE_NANOS, 3L).isValid(), "range rejected");
    }

    private static void snapshotDefensivelyCopiesArrays() {
        byte[] self = {1,2,3};
        ObservationSnapshot snapshot = snapshot(self, ActionCommand.zero(2L, 2L, "zero"));
        self[0] = 9;
        require(snapshot.selfState()[0] == 1, "constructor copy");
        byte[] returned = snapshot.selfState(); returned[1] = 9;
        require(snapshot.selfState()[1] == 2, "accessor copy");
    }

    private static void snapshotRejectsMissingPreviousAction() {
        boolean rejected = false;
        try { snapshot(new byte[0], null); } catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected, "missing previous action rejected");
    }

    private static ObservationSnapshot snapshot(byte[] self, ActionCommand action) {
        return new ObservationSnapshot(1L, 2L, UUID.randomUUID(), 3L,
            "world-hash", "universal", self, new byte[0], new byte[0], new byte[0], new byte[0],
            new byte[0], new byte[0], new byte[0], new byte[0], action, 0L);
    }

    private static void rollingWindowRemainsBounded() {
        RollingTimingWindow window = new RollingTimingWindow(3);
        window.add(10); window.add(20); window.add(30); window.add(40);
        require(window.count() == 3, "bounded count");
        require(window.averageNanos() == 30, "ring average");
        require(window.latestNanos() == 40, "latest");
        require(window.maximumNanos() == 40, "maximum");
    }

    private static void safetyControllerReleasesEveryHeldControl() {
        Minecraft minecraft = Minecraft.getMinecraft();
        int[] keys = {1,2,3,4,5,6,7,8,9,10,11};
        for (int key : keys) KeyBinding.setKeyBindState(key, true);
        SawBotStateController controller = new SawBotStateController(minecraft, new TestLogger());
        controller.toggleEnabled();
        require(controller.mode() == SawBotMode.ENABLED, "controller enabled");
        controller.emergencyStop();
        require(controller.mode() == SawBotMode.DISABLED, "controller disabled");
        require("emergency stop".equals(controller.lastStopReason()), "stop reason");
        for (int key : keys) require(!KeyBinding.isKeyDownForTest(key), "key released " + key);
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    private static final class TestLogger implements Logger {
        public void info(String message) { }
        public void info(String message, Object value) { }
        public void warn(String message) { }
        public void error(String message, Throwable throwable) { }
    }
}
