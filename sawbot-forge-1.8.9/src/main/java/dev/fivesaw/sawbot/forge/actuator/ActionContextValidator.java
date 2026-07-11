package dev.fivesaw.sawbot.forge.actuator;

import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.action.ActionValidation;
import dev.fivesaw.sawbot.common.action.ActionValidator;
import dev.fivesaw.sawbot.common.observation.EntityObservation;
import dev.fivesaw.sawbot.common.observation.LandmarkObservation;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;

/** Adds snapshot-reference checks that the structure-only common validator cannot perform. */
public final class ActionContextValidator {
    private ActionContextValidator() { }

    public static ActionValidation validate(ActionCommand command, ObservationSnapshot latest,
                                            long nowNanos, long maxAgeNanos, long maxSequenceLag) {
        if (latest == null) return ActionValidation.invalid("no observation");
        ActionValidation structural = ActionValidator.validate(command, latest.sequenceNumber(),
            nowNanos, maxAgeNanos, maxSequenceLag);
        if (!structural.isValid()) return structural;
        if (command.selectedTargetTrackingId() >= 0 && !hasEntity(latest, command.selectedTargetTrackingId())) {
            return ActionValidation.invalid("selected target unavailable");
        }
        if (command.selectedWaypointId() >= 0 && !hasLandmark(latest, command.selectedWaypointId())) {
            return ActionValidation.invalid("selected waypoint unavailable");
        }
        return ActionValidation.valid();
    }

    private static boolean hasEntity(ObservationSnapshot snapshot, int trackingId) {
        for (EntityObservation entity : snapshot.entities().entities()) {
            if (entity.trackingId() == trackingId) return true;
        }
        return false;
    }

    private static boolean hasLandmark(ObservationSnapshot snapshot, int landmarkId) {
        for (LandmarkObservation landmark : snapshot.landmarks().landmarks()) {
            if (landmark.landmarkId() == landmarkId) return true;
        }
        return false;
    }
}
