package dev.fivesaw.sawbot.forge.telemetry;

import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.events.ObservationEvent;
import dev.fivesaw.sawbot.common.observation.EntityObservation;
import dev.fivesaw.sawbot.common.observation.InventorySnapshot;
import dev.fivesaw.sawbot.common.observation.ItemSlotObservation;
import dev.fivesaw.sawbot.common.observation.LandmarkObservation;
import dev.fivesaw.sawbot.common.observation.LocalTerrainSnapshot;
import dev.fivesaw.sawbot.common.observation.MidRangeMapSnapshot;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.common.observation.SelfState;
import dev.fivesaw.sawbot.common.observation.SensorTimings;
import dev.fivesaw.sawbot.common.observation.ServerTimingSnapshot;
import dev.fivesaw.sawbot.common.telemetry.HumanInputSample;
import dev.fivesaw.sawbot.common.telemetry.TrajectoryStep;

/** Deterministic binary encoder used by the high-volume trajectory format. */
public final class TelemetryBinaryCodec {
    public static final int STEP_PAYLOAD_MAGIC = 0x31505453; // STP1 in little endian bytes.
    public static final int FOOTER_PAYLOAD_MAGIC = 0x31525446; // FTR1.
    public static final int OBSERVATION_PAYLOAD_MAGIC = 0x3153424F; // OBS1.
    public static final int PAYLOAD_VERSION = 1;

    private TelemetryBinaryCodec() { }

    public static byte[] encodeStep(TrajectoryStep step) {
        if (step == null) throw new IllegalArgumentException("step");
        LittleEndianOutput out = new LittleEndianOutput(24576);
        ObservationSnapshot observation = step.observation();

        out.writeInt(STEP_PAYLOAD_MAGIC);
        out.writeShort(PAYLOAD_VERSION);
        out.writeByte(step.actionSource().ordinal());
        out.writeBoolean(step.incompleteOutcome());
        out.writeLong(observation.sequenceNumber());
        out.writeLong(observation.clientTick());
        out.writeLong(step.outcomeSequenceNumber());
        out.writeLong(step.outcomeClientTick());
        out.writeShort(step.humanInput().count());
        out.writeInt(step.humanInput().droppedSamples());
        out.writeShort(step.outcomeEvents().size());
        out.writeShort(0);
        int keyOr = 0;
        int mouseX = 0;
        int mouseY = 0;
        int guiSamples = 0;
        int firstSlot = observation.selfState().selectedSlot();
        boolean first = true;
        for (HumanInputSample sample : step.humanInput().samples()) {
            keyOr |= sample.keyBits();
            mouseX += sample.mouseDeltaX();
            mouseY += sample.mouseDeltaY();
            if (sample.guiOpen()) guiSamples++;
            if (first) { firstSlot = sample.selectedSlot(); first = false; }
        }
        out.writeShort(keyOr);
        out.writeInt(mouseX);
        out.writeInt(mouseY);
        out.writeByte(firstSlot);
        out.writeByte(guiSamples);

        writeObservation(out, observation);

        for (HumanInputSample sample : step.humanInput().samples()) {
            out.writeLong(sample.clientTick());
            out.writeLong(sample.monotonicTimestampNanos());
            out.writeShort(sample.keyBits());
            out.writeInt(sample.mouseDeltaX());
            out.writeInt(sample.mouseDeltaY());
            out.writeByte(sample.selectedSlot());
            out.writeBoolean(sample.guiOpen());
        }

        for (ObservationEvent event : step.outcomeEvents()) writeEvent(out, event);
        return out.toByteArray();
    }

    /** Full immutable observation payload for the local model bridge. */
    public static byte[] encodeObservation(ObservationSnapshot observation) {
        if (observation == null) throw new IllegalArgumentException("observation");
        LittleEndianOutput out = new LittleEndianOutput(24576);
        out.writeInt(OBSERVATION_PAYLOAD_MAGIC);
        out.writeShort(PAYLOAD_VERSION);
        out.writeShort(0);
        out.writeLong(observation.sequenceNumber());
        out.writeLong(observation.clientTick());
        writeObservation(out, observation);
        return out.toByteArray();
    }

    public static byte[] encodeFooter(long stepCount, long droppedSteps, int rollingCrc32,
                                      String terminalReason) {
        LittleEndianOutput out = new LittleEndianOutput(96);
        out.writeInt(FOOTER_PAYLOAD_MAGIC);
        out.writeShort(PAYLOAD_VERSION);
        out.writeShort(0);
        out.writeLong(stepCount);
        out.writeLong(droppedSteps);
        out.writeInt(rollingCrc32);
        out.writeUtf8(terminalReason == null ? "unknown" : terminalReason, 96);
        return out.toByteArray();
    }

    private static void writeObservation(LittleEndianOutput out, ObservationSnapshot snapshot) {
        out.writeUtf8(snapshot.schemaVersion().identifier(), 48);
        out.writeLong(snapshot.monotonicTimestampNanos());
        out.writeLong(snapshot.episodeId().getMostSignificantBits());
        out.writeLong(snapshot.episodeId().getLeastSignificantBits());
        out.writeUtf8(snapshot.worldIdentifier(), 96);
        out.writeUtf8(snapshot.taskAdapterIdentifier(), 48);
        out.writeLong(snapshot.sensorValidityFlags());

        writeSelf(out, snapshot.selfState());
        writeTerrain(out, snapshot.localTerrain());
        writeMidRange(out, snapshot.midRangeMap());

        out.writeShort(snapshot.entities().count());
        out.writeShort(snapshot.entities().droppedCount());
        for (EntityObservation entity : snapshot.entities().entities()) writeEntity(out, entity);

        writeInventory(out, snapshot.inventory());

        out.writeShort(snapshot.landmarks().count());
        for (LandmarkObservation landmark : snapshot.landmarks().landmarks()) writeLandmark(out, landmark);

        out.writeShort(snapshot.events().count());
        out.writeLong(snapshot.events().dropped());
        for (ObservationEvent event : snapshot.events().events()) writeEvent(out, event);

        writeServerTiming(out, snapshot.serverTiming());
        out.writeUtf8(snapshot.taskState().adapterIdentifier(), 48);
        out.writeBoolean(snapshot.taskState().taskActive());
        writeAction(out, snapshot.previousAction());
        writeSensorTimings(out, snapshot.sensorTimings());
    }

    private static void writeSelf(LittleEndianOutput out, SelfState self) {
        out.writeFloat(self.health());
        out.writeFloat(self.absorption());
        out.writeFloat(self.hunger());
        out.writeFloat(self.armour());
        out.writeDouble(self.absoluteX());
        out.writeDouble(self.absoluteY());
        out.writeDouble(self.absoluteZ());
        out.writeFloat(self.velocityRight());
        out.writeFloat(self.velocityUp());
        out.writeFloat(self.velocityForward());
        out.writeFloat(self.accelerationRight());
        out.writeFloat(self.accelerationUp());
        out.writeFloat(self.accelerationForward());
        out.writeFloat(self.yawDegrees());
        out.writeFloat(self.pitchDegrees());
        out.writeFloat(self.fallDistance());
        int bits = 0;
        if (self.onGround()) bits |= 1 << 0;
        if (self.horizontalCollision()) bits |= 1 << 1;
        if (self.verticalCollision()) bits |= 1 << 2;
        if (self.inLiquid()) bits |= 1 << 3;
        if (self.onLadder()) bits |= 1 << 4;
        if (self.insideBlock()) bits |= 1 << 5;
        if (self.sprinting()) bits |= 1 << 6;
        if (self.sneaking()) bits |= 1 << 7;
        if (self.usingItem()) bits |= 1 << 8;
        out.writeShort(bits);
        out.writeInt(self.airborneTicks());
        out.writeInt(self.hurtTimerTicks());
        out.writeByte(self.selectedSlot());
        out.writeFloat(self.supportDistanceLeft());
        out.writeFloat(self.supportDistanceCenter());
        out.writeFloat(self.supportDistanceRight());
        out.writeFloat(self.distanceToVoid());
        out.writeShort(self.activePotionCount());
    }

    private static void writeTerrain(LittleEndianOutput out, LocalTerrainSnapshot terrain) {
        out.writeInt(terrain.originX());
        out.writeInt(terrain.originY());
        out.writeInt(terrain.originZ());
        out.writeByte(terrain.facingQuadrant());
        out.writeShort(terrain.changedCellCount());
        short[] ids = terrain.blockStateIds();
        for (short value : ids) out.writeShort(value & 0xFFFF);
        out.writeBytes(terrain.categories());
        short[] flags = terrain.flags();
        for (short value : flags) out.writeShort(value & 0xFFFF);
        out.writeBytes(terrain.collisionHeightClasses());
    }

    private static void writeMidRange(LittleEndianOutput out, MidRangeMapSnapshot map) {
        out.writeInt(map.originX());
        out.writeInt(map.originY());
        out.writeInt(map.originZ());
        out.writeByte(map.facingQuadrant());
        out.writeByte(map.rowsUpdatedThisTick());
        for (short value : map.relativeSurfaceY()) out.writeShort(value & 0xFFFF);
        for (short value : map.flags()) out.writeShort(value & 0xFFFF);
        for (short value : map.ageTicks()) out.writeShort(value & 0xFFFF);
    }

    private static void writeEntity(LittleEndianOutput out, EntityObservation entity) {
        out.writeInt(entity.trackingId());
        out.writeInt(entity.minecraftEntityId());
        out.writeByte(entity.kind().ordinal());
        out.writeByte(entity.type().ordinal());
        out.writeByte(entity.teamRelation().ordinal());
        out.writeFloat(entity.right());
        out.writeFloat(entity.up());
        out.writeFloat(entity.forward());
        out.writeFloat(entity.velocityRight());
        out.writeFloat(entity.velocityUp());
        out.writeFloat(entity.velocityForward());
        out.writeFloat(entity.accelerationRight());
        out.writeFloat(entity.accelerationUp());
        out.writeFloat(entity.accelerationForward());
        out.writeFloat(entity.yawDegrees());
        out.writeFloat(entity.pitchDegrees());
        out.writeFloat(entity.width());
        out.writeFloat(entity.height());
        out.writeFloat(entity.health());
        out.writeFloat(entity.armour());
        out.writeFloat(entity.distance());
        out.writeFloat(entity.trackingConfidence());
        out.writeByte(entity.heldItemCategory());
        out.writeByte(entity.payloadItemCategory());
        out.writeShort(entity.hurtTimerTicks());
        int bits = 0;
        if (entity.onGround()) bits |= 1 << 0;
        if (entity.sprinting()) bits |= 1 << 1;
        if (entity.sneaking()) bits |= 1 << 2;
        if (entity.lineOfSight()) bits |= 1 << 3;
        if (entity.occluded()) bits |= 1 << 4;
        if (entity.attackable()) bits |= 1 << 5;
        if (entity.loaded()) bits |= 1 << 6;
        out.writeByte(bits);
    }

    private static void writeInventory(LittleEndianOutput out, InventorySnapshot inventory) {
        out.writeByte(inventory.selectedSlot());
        out.writeUtf8(inventory.openContainerType(), 48);
        out.writeInt(inventory.iron());
        out.writeInt(inventory.gold());
        out.writeInt(inventory.diamonds());
        out.writeInt(inventory.emeralds());
        out.writeInt(inventory.wool());
        out.writeByte(inventory.slots().size());
        for (ItemSlotObservation slot : inventory.slots()) {
            out.writeByte(slot.slotIndex());
            out.writeInt(slot.itemId());
            out.writeShort(slot.metadata());
            out.writeByte(slot.count());
            out.writeByte(slot.durabilityClass());
            out.writeInt(slot.enchantmentBits());
            out.writeByte(slot.category().ordinal());
        }
    }

    private static void writeLandmark(LittleEndianOutput out, LandmarkObservation landmark) {
        out.writeInt(landmark.landmarkId());
        out.writeByte(landmark.type().ordinal());
        out.writeByte(landmark.team().ordinal());
        out.writeFloat(landmark.right());
        out.writeFloat(landmark.up());
        out.writeFloat(landmark.forward());
        out.writeFloat(landmark.distance());
        out.writeFloat(landmark.estimatedTravelCost());
        out.writeFloat(landmark.danger());
        out.writeFloat(landmark.value());
        out.writeFloat(landmark.confidence());
        out.writeBoolean(landmark.reachable());
    }

    private static void writeEvent(LittleEndianOutput out, ObservationEvent event) {
        out.writeByte(event.type().ordinal());
        out.writeLong(event.clientTick());
        out.writeInt(event.trackingId());
        out.writeFloat(event.right());
        out.writeFloat(event.up());
        out.writeFloat(event.forward());
        out.writeFloat(event.magnitude());
        out.writeBoolean(event.success());
    }

    private static void writeServerTiming(LittleEndianOutput out, ServerTimingSnapshot timing) {
        out.writeInt(timing.estimatedPingMillis());
        out.writeInt(timing.pingJitterMillis());
        out.writeInt(timing.ticksSinceEnemyUpdate());
        out.writeInt(timing.ticksSinceHitConfirmation());
        out.writeInt(timing.ticksSincePlacementAcknowledgement());
        out.writeInt(timing.ticksSinceServerCorrection());
        out.writeBoolean(timing.pingValid());
    }

    private static void writeAction(LittleEndianOutput out, ActionCommand action) {
        out.writeUtf8(action.schemaVersion().identifier(), 48);
        out.writeLong(action.observationSequenceNumber());
        out.writeLong(action.generatedTimestampNanos());
        out.writeUtf8(action.modelVersion() == null ? "unknown" : action.modelVersion(), 96);
        out.writeFloat(action.forward());
        out.writeFloat(action.strafe());
        out.writeFloat(action.yawDeltaDegrees());
        out.writeFloat(action.pitchDeltaDegrees());
        out.writeFloat(action.jumpProbability());
        out.writeFloat(action.sprintProbability());
        out.writeFloat(action.sneakProbability());
        out.writeFloat(action.attackProbability());
        out.writeFloat(action.useOrPlaceProbability());
        out.writeFloat(action.dropProbability());
        out.writeFloat(action.inventoryToggleProbability());
        out.writeByte(action.hotbarSlot() + 1);
        out.writeByte(action.selectedSkill().ordinal());
        out.writeInt(action.selectedTargetTrackingId());
        out.writeInt(action.selectedWaypointId());
        out.writeFloat(action.confidence());
        out.writeShort(action.actionDurationTicks());
        out.writeByte(action.tacticalObjective().ordinal());
        out.writeByte(action.abortCondition().ordinal());
    }

    private static void writeSensorTimings(LittleEndianOutput out, SensorTimings timings) {
        out.writeLong(timings.selfNanos());
        out.writeLong(timings.terrainNanos());
        out.writeLong(timings.midRangeNanos());
        out.writeLong(timings.entitiesNanos());
        out.writeLong(timings.inventoryNanos());
        out.writeLong(timings.landmarksNanos());
        out.writeLong(timings.eventsNanos());
        out.writeLong(timings.serverTimingNanos());
        out.writeLong(timings.totalNanos());
    }
}
