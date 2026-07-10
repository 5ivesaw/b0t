package dev.fivesaw.sawbot.forge.inspection;

import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.events.ObservationEvent;
import dev.fivesaw.sawbot.common.observation.EntityObservation;
import dev.fivesaw.sawbot.common.observation.InventorySnapshot;
import dev.fivesaw.sawbot.common.observation.ItemSlotObservation;
import dev.fivesaw.sawbot.common.observation.LandmarkObservation;
import dev.fivesaw.sawbot.common.observation.LocalTerrainSnapshot;
import dev.fivesaw.sawbot.common.observation.MidRangeMapSnapshot;
import dev.fivesaw.sawbot.common.observation.ObservationDiff;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.common.observation.SelfState;
import dev.fivesaw.sawbot.common.observation.SensorTimings;
import dev.fivesaw.sawbot.common.observation.ServerTimingSnapshot;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/** Dependency-free human-readable JSON exporter for individual immutable snapshots. */
public final class SnapshotJsonWriter {
    private SnapshotJsonWriter() { }

    public static void write(ObservationSnapshot snapshot, ObservationDiff diff,
                             BlockInspection block, int selectedTrackingId,
                             Writer writer) throws IOException {
        if (snapshot == null || writer == null) throw new IllegalArgumentException("snapshot/writer");
        Json out = new Json(writer);
        out.raw("{\n");
        out.field("exportFormat", "sawbot.snapshot.debug/0.1", true, 1);
        out.field("schemaVersion", snapshot.schemaVersion().identifier(), true, 1);
        out.field("clientTick", snapshot.clientTick(), true, 1);
        out.field("monotonicTimestampNanos", snapshot.monotonicTimestampNanos(), true, 1);
        out.field("episodeId", snapshot.episodeId().toString(), true, 1);
        out.field("sequenceNumber", snapshot.sequenceNumber(), true, 1);
        out.field("worldIdentifier", snapshot.worldIdentifier(), true, 1);
        out.field("taskAdapterIdentifier", snapshot.taskAdapterIdentifier(), true, 1);
        out.field("sensorValidityFlagsHex", "0x" + Long.toHexString(snapshot.sensorValidityFlags()), true, 1);

        out.indent(1); out.raw("\"selfState\": {"); writeSelf(snapshot.selfState(), out, 2); out.raw("\n  },\n");
        out.indent(1); out.raw("\"localTerrain\": {"); writeTerrain(snapshot.localTerrain(), out, 2); out.raw("\n  },\n");
        out.indent(1); out.raw("\"midRangeMap\": {"); writeMap(snapshot.midRangeMap(), out, 2); out.raw("\n  },\n");
        out.indent(1); out.raw("\"entities\": ["); writeEntities(snapshot.entities().entities(), out, 2); out.raw("\n  ],\n");
        out.indent(1); out.raw("\"inventory\": {"); writeInventory(snapshot.inventory(), out, 2); out.raw("\n  },\n");
        out.indent(1); out.raw("\"landmarks\": ["); writeLandmarks(snapshot.landmarks().landmarks(), out, 2); out.raw("\n  ],\n");
        out.indent(1); out.raw("\"events\": ["); writeEvents(snapshot.events().events(), out, 2); out.raw("\n  ],\n");
        out.indent(1); out.raw("\"serverTiming\": {"); writeTiming(snapshot.serverTiming(), out, 2); out.raw("\n  },\n");
        out.indent(1); out.raw("\"taskState\": {\n");
        out.field("adapterIdentifier", snapshot.taskState().adapterIdentifier(), true, 2);
        out.field("taskActive", snapshot.taskState().taskActive(), false, 2);
        out.indent(1); out.raw("},\n");
        out.indent(1); out.raw("\"previousAction\": {"); writeAction(snapshot.previousAction(), out, 2); out.raw("\n  },\n");
        out.indent(1); out.raw("\"sensorTimingsNanos\": {"); writeSensorTimings(snapshot.sensorTimings(), out, 2); out.raw("\n  },\n");
        out.indent(1); out.raw("\"differenceFromPrevious\": {"); writeDiff(diff == null ? ObservationDiff.EMPTY : diff, out, 2); out.raw("\n  },\n");
        out.indent(1); out.raw("\"inspectorSelection\": {\n");
        out.field("selectedTrackingId", selectedTrackingId, block != null, 2);
        if (block != null) {
            out.indent(2); out.raw("\"block\": {\n");
            out.field("worldX", block.worldX(), true, 3);
            out.field("worldY", block.worldY(), true, 3);
            out.field("worldZ", block.worldZ(), true, 3);
            out.field("rightOffset", block.rightOffset(), true, 3);
            out.field("upOffset", block.upOffset(), true, 3);
            out.field("forwardOffset", block.forwardOffset(), true, 3);
            out.field("insideTensor", block.insideTensor(), true, 3);
            out.field("terrainIndex", block.terrainIndex(), true, 3);
            out.field("blockStateId", block.blockStateId(), true, 3);
            out.field("category", block.category().name(), true, 3);
            out.field("flagsHex", "0x" + Integer.toHexString(block.flags() & 0xFFFF), true, 3);
            out.field("collisionHeightClass", block.collisionHeightClass(), false, 3);
            out.indent(2); out.raw("}\n");
        }
        out.indent(1); out.raw("}\n");
        out.raw("}\n");
        writer.flush();
    }

    private static void writeSelf(SelfState s, Json out, int level) throws IOException {
        out.raw("\n");
        out.field("health", s.health(), true, level);
        out.field("absorption", s.absorption(), true, level);
        out.field("hunger", s.hunger(), true, level);
        out.field("armour", s.armour(), true, level);
        out.field("absoluteX", s.absoluteX(), true, level);
        out.field("absoluteY", s.absoluteY(), true, level);
        out.field("absoluteZ", s.absoluteZ(), true, level);
        out.field("velocityRight", s.velocityRight(), true, level);
        out.field("velocityUp", s.velocityUp(), true, level);
        out.field("velocityForward", s.velocityForward(), true, level);
        out.field("accelerationRight", s.accelerationRight(), true, level);
        out.field("accelerationUp", s.accelerationUp(), true, level);
        out.field("accelerationForward", s.accelerationForward(), true, level);
        out.field("yawDegrees", s.yawDegrees(), true, level);
        out.field("pitchDegrees", s.pitchDegrees(), true, level);
        out.field("fallDistance", s.fallDistance(), true, level);
        out.field("onGround", s.onGround(), true, level);
        out.field("horizontalCollision", s.horizontalCollision(), true, level);
        out.field("verticalCollision", s.verticalCollision(), true, level);
        out.field("inLiquid", s.inLiquid(), true, level);
        out.field("onLadder", s.onLadder(), true, level);
        out.field("insideBlock", s.insideBlock(), true, level);
        out.field("sprinting", s.sprinting(), true, level);
        out.field("sneaking", s.sneaking(), true, level);
        out.field("usingItem", s.usingItem(), true, level);
        out.field("airborneTicks", s.airborneTicks(), true, level);
        out.field("hurtTimerTicks", s.hurtTimerTicks(), true, level);
        out.field("selectedSlot", s.selectedSlot(), true, level);
        out.field("supportDistanceLeft", s.supportDistanceLeft(), true, level);
        out.field("supportDistanceCenter", s.supportDistanceCenter(), true, level);
        out.field("supportDistanceRight", s.supportDistanceRight(), true, level);
        out.field("distanceToVoid", s.distanceToVoid(), true, level);
        out.field("activePotionCount", s.activePotionCount(), false, level);
    }

    private static void writeTerrain(LocalTerrainSnapshot t, Json out, int level) throws IOException {
        out.raw("\n");
        out.field("originX", t.originX(), true, level);
        out.field("originY", t.originY(), true, level);
        out.field("originZ", t.originZ(), true, level);
        out.field("facingQuadrant", t.facingQuadrant(), true, level);
        out.field("changedCellCount", t.changedCellCount(), true, level);
        out.array("blockStateIds", t.blockStateIds(), true, level);
        out.array("categories", t.categories(), true, level);
        out.array("flags", t.flags(), true, level);
        out.array("collisionHeightClasses", t.collisionHeightClasses(), false, level);
    }

    private static void writeMap(MidRangeMapSnapshot m, Json out, int level) throws IOException {
        out.raw("\n");
        out.field("originX", m.originX(), true, level);
        out.field("originY", m.originY(), true, level);
        out.field("originZ", m.originZ(), true, level);
        out.field("facingQuadrant", m.facingQuadrant(), true, level);
        out.field("rowsUpdatedThisTick", m.rowsUpdatedThisTick(), true, level);
        out.arraySigned("relativeSurfaceY", m.relativeSurfaceY(), true, level);
        out.array("flags", m.flags(), true, level);
        out.array("ageTicks", m.ageTicks(), false, level);
    }

    private static void writeEntities(List<EntityObservation> entities, Json out, int level) throws IOException {
        if (entities.isEmpty()) return;
        out.raw("\n");
        for (int i = 0; i < entities.size(); i++) {
            EntityObservation e = entities.get(i);
            out.indent(level); out.raw("{\n");
            out.field("trackingId", e.trackingId(), true, level + 1);
            out.field("minecraftEntityId", e.minecraftEntityId(), true, level + 1);
            out.field("kind", e.kind().name(), true, level + 1);
            out.field("teamRelation", e.teamRelation().name(), true, level + 1);
            out.field("right", e.right(), true, level + 1);
            out.field("up", e.up(), true, level + 1);
            out.field("forward", e.forward(), true, level + 1);
            out.field("velocityRight", e.velocityRight(), true, level + 1);
            out.field("velocityUp", e.velocityUp(), true, level + 1);
            out.field("velocityForward", e.velocityForward(), true, level + 1);
            out.field("accelerationRight", e.accelerationRight(), true, level + 1);
            out.field("accelerationUp", e.accelerationUp(), true, level + 1);
            out.field("accelerationForward", e.accelerationForward(), true, level + 1);
            out.field("yawDegrees", e.yawDegrees(), true, level + 1);
            out.field("pitchDegrees", e.pitchDegrees(), true, level + 1);
            out.field("width", e.width(), true, level + 1);
            out.field("height", e.height(), true, level + 1);
            out.field("health", e.health(), true, level + 1);
            out.field("armour", e.armour(), true, level + 1);
            out.field("distance", e.distance(), true, level + 1);
            out.field("heldItemCategory", e.heldItemCategory(), true, level + 1);
            out.field("hurtTimerTicks", e.hurtTimerTicks(), true, level + 1);
            out.field("onGround", e.onGround(), true, level + 1);
            out.field("sprinting", e.sprinting(), true, level + 1);
            out.field("sneaking", e.sneaking(), true, level + 1);
            out.field("lineOfSight", e.lineOfSight(), true, level + 1);
            out.field("occluded", e.occluded(), true, level + 1);
            out.field("attackable", e.attackable(), true, level + 1);
            out.field("loaded", e.loaded(), true, level + 1);
            out.field("trackingConfidence", e.trackingConfidence(), false, level + 1);
            out.indent(level); out.raw("}");
            out.raw(i + 1 < entities.size() ? ",\n" : "\n");
        }
        out.indent(level - 1);
    }

    private static void writeInventory(InventorySnapshot inventory, Json out, int level) throws IOException {
        out.raw("\n");
        out.field("selectedSlot", inventory.selectedSlot(), true, level);
        out.field("openContainerType", inventory.openContainerType(), true, level);
        out.field("iron", inventory.iron(), true, level);
        out.field("gold", inventory.gold(), true, level);
        out.field("diamonds", inventory.diamonds(), true, level);
        out.field("emeralds", inventory.emeralds(), true, level);
        out.field("wool", inventory.wool(), true, level);
        out.indent(level); out.raw("\"slots\": [\n");
        List<ItemSlotObservation> slots = inventory.slots();
        for (int i = 0; i < slots.size(); i++) {
            ItemSlotObservation s = slots.get(i);
            out.indent(level + 1); out.raw("{\"slotIndex\": " + s.slotIndex()
                + ", \"itemId\": " + s.itemId() + ", \"metadata\": " + s.metadata()
                + ", \"count\": " + s.count() + ", \"durabilityClass\": " + s.durabilityClass()
                + ", \"enchantmentBits\": " + s.enchantmentBits() + ", \"category\": ");
            out.string(s.category().name()); out.raw("}");
            out.raw(i + 1 < slots.size() ? ",\n" : "\n");
        }
        out.indent(level); out.raw("]\n");
    }

    private static void writeLandmarks(List<LandmarkObservation> landmarks, Json out, int level) throws IOException {
        if (landmarks.isEmpty()) return;
        out.raw("\n");
        for (int i = 0; i < landmarks.size(); i++) {
            LandmarkObservation l = landmarks.get(i);
            out.indent(level); out.raw("{\"landmarkId\": " + l.landmarkId() + ", \"type\": "); out.string(l.type().name());
            out.raw(", \"team\": "); out.string(l.team().name());
            out.raw(", \"right\": " + l.right() + ", \"up\": " + l.up() + ", \"forward\": " + l.forward()
                + ", \"distance\": " + l.distance() + ", \"estimatedTravelCost\": " + l.estimatedTravelCost()
                + ", \"danger\": " + l.danger() + ", \"value\": " + l.value()
                + ", \"confidence\": " + l.confidence() + ", \"reachable\": " + l.reachable() + "}");
            out.raw(i + 1 < landmarks.size() ? ",\n" : "\n");
        }
        out.indent(level - 1);
    }

    private static void writeEvents(List<ObservationEvent> events, Json out, int level) throws IOException {
        if (events.isEmpty()) return;
        out.raw("\n");
        for (int i = 0; i < events.size(); i++) {
            ObservationEvent e = events.get(i);
            out.indent(level); out.raw("{\"type\": "); out.string(e.type().name());
            out.raw(", \"clientTick\": " + e.clientTick() + ", \"trackingId\": " + e.trackingId()
                + ", \"right\": " + e.right() + ", \"up\": " + e.up()
                + ", \"forward\": " + e.forward() + ", \"magnitude\": " + e.magnitude()
                + ", \"success\": " + e.success() + "}");
            out.raw(i + 1 < events.size() ? ",\n" : "\n");
        }
        out.indent(level - 1);
    }

    private static void writeTiming(ServerTimingSnapshot s, Json out, int level) throws IOException {
        out.raw("\n");
        out.field("estimatedPingMillis", s.estimatedPingMillis(), true, level);
        out.field("pingJitterMillis", s.pingJitterMillis(), true, level);
        out.field("ticksSinceEnemyUpdate", s.ticksSinceEnemyUpdate(), true, level);
        out.field("ticksSinceHitConfirmation", s.ticksSinceHitConfirmation(), true, level);
        out.field("ticksSincePlacementAcknowledgement", s.ticksSincePlacementAcknowledgement(), true, level);
        out.field("ticksSinceServerCorrection", s.ticksSinceServerCorrection(), true, level);
        out.field("pingValid", s.pingValid(), false, level);
    }

    private static void writeAction(ActionCommand a, Json out, int level) throws IOException {
        out.raw("\n");
        out.field("schemaVersion", a.schemaVersion().identifier(), true, level);
        out.field("observationSequenceNumber", a.observationSequenceNumber(), true, level);
        out.field("generatedTimestampNanos", a.generatedTimestampNanos(), true, level);
        out.field("modelVersion", a.modelVersion(), true, level);
        out.field("forward", a.forward(), true, level);
        out.field("strafe", a.strafe(), true, level);
        out.field("yawDeltaDegrees", a.yawDeltaDegrees(), true, level);
        out.field("pitchDeltaDegrees", a.pitchDeltaDegrees(), true, level);
        out.field("jumpProbability", a.jumpProbability(), true, level);
        out.field("sprintProbability", a.sprintProbability(), true, level);
        out.field("sneakProbability", a.sneakProbability(), true, level);
        out.field("attackProbability", a.attackProbability(), true, level);
        out.field("useOrPlaceProbability", a.useOrPlaceProbability(), true, level);
        out.field("dropProbability", a.dropProbability(), true, level);
        out.field("inventoryToggleProbability", a.inventoryToggleProbability(), true, level);
        out.field("hotbarSlot", a.hotbarSlot(), true, level);
        out.field("selectedSkill", a.selectedSkill().name(), true, level);
        out.field("selectedTargetTrackingId", a.selectedTargetTrackingId(), true, level);
        out.field("selectedWaypointId", a.selectedWaypointId(), true, level);
        out.field("confidence", a.confidence(), true, level);
        out.field("actionDurationTicks", a.actionDurationTicks(), true, level);
        out.field("tacticalObjective", a.tacticalObjective().name(), true, level);
        out.field("abortCondition", a.abortCondition().name(), false, level);
    }

    private static void writeSensorTimings(SensorTimings t, Json out, int level) throws IOException {
        out.raw("\n");
        out.field("self", t.selfNanos(), true, level);
        out.field("terrain", t.terrainNanos(), true, level);
        out.field("midRange", t.midRangeNanos(), true, level);
        out.field("entities", t.entitiesNanos(), true, level);
        out.field("inventory", t.inventoryNanos(), true, level);
        out.field("landmarks", t.landmarksNanos(), true, level);
        out.field("events", t.eventsNanos(), true, level);
        out.field("serverTiming", t.serverTimingNanos(), true, level);
        out.field("total", t.totalNanos(), false, level);
    }

    private static void writeDiff(ObservationDiff d, Json out, int level) throws IOException {
        out.raw("\n");
        out.field("fromSequence", d.fromSequence(), true, level);
        out.field("toSequence", d.toSequence(), true, level);
        out.field("clientTickDelta", d.clientTickDelta(), true, level);
        out.field("positionDistance", d.positionDistance(), true, level);
        out.field("yawDeltaDegrees", d.yawDeltaDegrees(), true, level);
        out.field("terrainChangedCells", d.terrainChangedCells(), true, level);
        out.field("mapChangedColumns", d.mapChangedColumns(), true, level);
        out.field("entitiesAdded", d.entitiesAdded(), true, level);
        out.field("entitiesRemoved", d.entitiesRemoved(), true, level);
        out.field("entitiesChanged", d.entitiesChanged(), true, level);
        out.field("inventoryChangedSlots", d.inventoryChangedSlots(), true, level);
        out.field("validityChangedBitsHex", "0x" + Long.toHexString(d.validityChangedBits()), false, level);
    }

    private static final class Json {
        private final Writer writer;
        Json(Writer writer) { this.writer = writer; }
        void raw(String value) throws IOException { writer.write(value); }
        void indent(int level) throws IOException { for (int i = 0; i < level; i++) writer.write("  "); }
        void string(String value) throws IOException {
            writer.write('"');
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '"': writer.write("\\\""); break;
                    case '\\': writer.write("\\\\"); break;
                    case '\b': writer.write("\\b"); break;
                    case '\f': writer.write("\\f"); break;
                    case '\n': writer.write("\\n"); break;
                    case '\r': writer.write("\\r"); break;
                    case '\t': writer.write("\\t"); break;
                    default:
                        if (c < 0x20) writer.write(String.format("\\u%04x", Integer.valueOf(c)));
                        else writer.write(c);
                }
            }
            writer.write('"');
        }
        void field(String name, String value, boolean comma, int level) throws IOException { indent(level); string(name); raw(": "); string(value); raw(comma ? ",\n" : "\n"); }
        void field(String name, long value, boolean comma, int level) throws IOException { indent(level); string(name); raw(": " + value + (comma ? ",\n" : "\n")); }
        void field(String name, double value, boolean comma, int level) throws IOException { indent(level); string(name); raw(": " + value + (comma ? ",\n" : "\n")); }
        void field(String name, boolean value, boolean comma, int level) throws IOException { indent(level); string(name); raw(": " + value + (comma ? ",\n" : "\n")); }
        void array(String name, short[] values, boolean comma, int level) throws IOException { indent(level); string(name); raw(": ["); for (int i=0;i<values.length;i++){if(i>0)raw(",");raw(Integer.toString(values[i] & 0xFFFF));} raw(comma ? "],\n" : "]\n"); }
        void arraySigned(String name, short[] values, boolean comma, int level) throws IOException { indent(level); string(name); raw(": ["); for (int i=0;i<values.length;i++){if(i>0)raw(",");raw(Short.toString(values[i]));} raw(comma ? "],\n" : "]\n"); }
        void array(String name, byte[] values, boolean comma, int level) throws IOException { indent(level); string(name); raw(": ["); for (int i=0;i<values.length;i++){if(i>0)raw(",");raw(Integer.toString(values[i] & 0xFF));} raw(comma ? "],\n" : "]\n"); }
    }
}
