package dev.fivesaw.sawbot.common.observation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure deterministic comparison utility used by the Phase 2 inspector and exports. */
public final class ObservationDiffCalculator {
    private ObservationDiffCalculator() { }

    public static ObservationDiff compare(ObservationSnapshot before, ObservationSnapshot after) {
        if (before == null || after == null) return ObservationDiff.EMPTY;
        if (!before.episodeId().equals(after.episodeId())) {
            return new ObservationDiff(before.sequenceNumber(), after.sequenceNumber(),
                Math.max(0L, after.clientTick() - before.clientTick()),
                distance(before.selfState(), after.selfState()),
                yawDelta(before.selfState().yawDegrees(), after.selfState().yawDegrees()),
                LocalTerrainSnapshot.CELL_COUNT, MidRangeMapSnapshot.COLUMN_COUNT,
                after.entities().count(), before.entities().count(), 0,
                InventorySnapshot.EXPECTED_SLOT_COUNT,
                before.sensorValidityFlags() ^ after.sensorValidityFlags());
        }

        return new ObservationDiff(before.sequenceNumber(), after.sequenceNumber(),
            Math.max(0L, after.clientTick() - before.clientTick()),
            distance(before.selfState(), after.selfState()),
            yawDelta(before.selfState().yawDegrees(), after.selfState().yawDegrees()),
            terrainChanges(before.localTerrain(), after.localTerrain()),
            mapChanges(before.midRangeMap(), after.midRangeMap()),
            entityAdded(before.entities(), after.entities()),
            entityRemoved(before.entities(), after.entities()),
            entityChanged(before.entities(), after.entities()),
            inventoryChanges(before.inventory(), after.inventory()),
            before.sensorValidityFlags() ^ after.sensorValidityFlags());
    }

    private static float distance(SelfState a, SelfState b) {
        double dx = b.absoluteX() - a.absoluteX();
        double dy = b.absoluteY() - a.absoluteY();
        double dz = b.absoluteZ() - a.absoluteZ();
        return (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static float yawDelta(float a, float b) {
        float value = (b - a) % 360f;
        if (value >= 180f) value -= 360f;
        if (value < -180f) value += 360f;
        return value;
    }

    private static int terrainChanges(LocalTerrainSnapshot a, LocalTerrainSnapshot b) {
        if (a.originX() != b.originX() || a.originY() != b.originY() || a.originZ() != b.originZ()
            || a.facingQuadrant() != b.facingQuadrant()) return LocalTerrainSnapshot.CELL_COUNT;
        int changed = 0;
        for (int index = 0; index < LocalTerrainSnapshot.CELL_COUNT; index++) {
            if (a.blockStateIdAt(index) != b.blockStateIdAt(index)
                || a.categoryAt(index) != b.categoryAt(index)
                || a.flagsAt(index) != b.flagsAt(index)
                || a.collisionHeightClassAt(index) != b.collisionHeightClassAt(index)) changed++;
        }
        return changed;
    }

    private static int mapChanges(MidRangeMapSnapshot a, MidRangeMapSnapshot b) {
        if (a.originX() != b.originX() || a.originY() != b.originY() || a.originZ() != b.originZ()
            || a.facingQuadrant() != b.facingQuadrant()) return MidRangeMapSnapshot.COLUMN_COUNT;
        int changed = 0;
        for (int index = 0; index < MidRangeMapSnapshot.COLUMN_COUNT; index++) {
            if (a.relativeSurfaceYAt(index) != b.relativeSurfaceYAt(index)
                || a.flagsAt(index) != b.flagsAt(index)) changed++;
        }
        return changed;
    }

    private static int inventoryChanges(InventorySnapshot a, InventorySnapshot b) {
        int changed = 0;
        List<ItemSlotObservation> left = a.slots();
        List<ItemSlotObservation> right = b.slots();
        for (int index = 0; index < InventorySnapshot.EXPECTED_SLOT_COUNT; index++) {
            if (!sameSlot(left.get(index), right.get(index))) changed++;
        }
        return changed;
    }

    private static boolean sameSlot(ItemSlotObservation a, ItemSlotObservation b) {
        return a.itemId() == b.itemId() && a.metadata() == b.metadata() && a.count() == b.count()
            && a.durabilityClass() == b.durabilityClass()
            && a.enchantmentBits() == b.enchantmentBits() && a.category() == b.category();
    }

    private static int entityAdded(EntitySetSnapshot before, EntitySetSnapshot after) {
        Map<Integer, EntityObservation> left = byTrackingId(before);
        int count = 0;
        for (EntityObservation entity : after.entities()) if (!left.containsKey(Integer.valueOf(entity.trackingId()))) count++;
        return count;
    }

    private static int entityRemoved(EntitySetSnapshot before, EntitySetSnapshot after) {
        Map<Integer, EntityObservation> right = byTrackingId(after);
        int count = 0;
        for (EntityObservation entity : before.entities()) if (!right.containsKey(Integer.valueOf(entity.trackingId()))) count++;
        return count;
    }

    private static int entityChanged(EntitySetSnapshot before, EntitySetSnapshot after) {
        Map<Integer, EntityObservation> left = byTrackingId(before);
        int count = 0;
        for (EntityObservation entity : after.entities()) {
            EntityObservation prior = left.get(Integer.valueOf(entity.trackingId()));
            if (prior != null && !sameEntity(prior, entity)) count++;
        }
        return count;
    }

    private static Map<Integer, EntityObservation> byTrackingId(EntitySetSnapshot set) {
        Map<Integer, EntityObservation> result = new HashMap<Integer, EntityObservation>();
        for (EntityObservation entity : set.entities()) result.put(Integer.valueOf(entity.trackingId()), entity);
        return result;
    }

    private static boolean sameEntity(EntityObservation a, EntityObservation b) {
        return a.minecraftEntityId() == b.minecraftEntityId() && a.kind() == b.kind()
            && a.teamRelation() == b.teamRelation()
            && bits(a.right()) == bits(b.right()) && bits(a.up()) == bits(b.up())
            && bits(a.forward()) == bits(b.forward())
            && bits(a.velocityRight()) == bits(b.velocityRight())
            && bits(a.velocityUp()) == bits(b.velocityUp())
            && bits(a.velocityForward()) == bits(b.velocityForward())
            && bits(a.health()) == bits(b.health()) && bits(a.armour()) == bits(b.armour())
            && bits(a.distance()) == bits(b.distance())
            && a.heldItemCategory() == b.heldItemCategory()
            && a.hurtTimerTicks() == b.hurtTimerTicks()
            && a.onGround() == b.onGround() && a.sprinting() == b.sprinting()
            && a.sneaking() == b.sneaking() && a.lineOfSight() == b.lineOfSight()
            && a.occluded() == b.occluded() && a.attackable() == b.attackable()
            && a.loaded() == b.loaded();
    }

    private static int bits(float value) { return Float.floatToIntBits(value); }
}
