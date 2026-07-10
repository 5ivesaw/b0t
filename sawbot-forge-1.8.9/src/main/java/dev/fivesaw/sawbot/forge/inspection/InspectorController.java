package dev.fivesaw.sawbot.forge.inspection;

import dev.fivesaw.sawbot.common.observation.BlockSemanticCategory;
import dev.fivesaw.sawbot.common.observation.EgocentricTransform;
import dev.fivesaw.sawbot.common.observation.EntityObservation;
import dev.fivesaw.sawbot.common.observation.LocalTerrainSnapshot;
import dev.fivesaw.sawbot.common.observation.ObservationDiff;
import dev.fivesaw.sawbot.common.observation.ObservationDiffCalculator;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

/** Client-thread-only selection and inspector-page state. */
public final class InspectorController {
    private final Minecraft minecraft;
    private InspectorPage page = InspectorPage.SUMMARY;
    private int selectedTrackingId = -1;
    private BlockInspection selectedBlock;
    private ObservationDiff latestDiff = ObservationDiff.EMPTY;

    public InspectorController(Minecraft minecraft) {
        if (minecraft == null) throw new IllegalArgumentException("minecraft");
        this.minecraft = minecraft;
    }

    public void update(ObservationSnapshot current, ObservationSnapshot previous) {
        latestDiff = ObservationDiffCalculator.compare(previous, current);
        if (current == null) {
            selectedTrackingId = -1;
            selectedBlock = null;
            return;
        }
        updateCrosshairSelection(current);
        if (findEntity(current, selectedTrackingId) == null && current.entities().count() > 0) {
            selectedTrackingId = current.entities().entities().get(0).trackingId();
        }
    }

    private void updateCrosshairSelection(ObservationSnapshot snapshot) {
        MovingObjectPosition hit = minecraft.objectMouseOver;
        if (hit == null || hit.typeOfHit == null) return;
        if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && hit.entityHit != null) {
            int minecraftId = hit.entityHit.getEntityId();
            for (EntityObservation entity : snapshot.entities().entities()) {
                if (entity.minecraftEntityId() == minecraftId) {
                    selectedTrackingId = entity.trackingId();
                    break;
                }
            }
        } else if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos position = hit.getBlockPos();
            if (position != null) selectedBlock = inspectBlock(snapshot.localTerrain(), position);
        }
    }

    public static BlockInspection inspectBlock(LocalTerrainSnapshot terrain, BlockPos position) {
        int dx = position.getX() - terrain.originX();
        int dz = position.getZ() - terrain.originZ();
        int right = EgocentricTransform.rightFromWorldDelta(dx, dz, terrain.facingQuadrant());
        int forward = EgocentricTransform.forwardFromWorldDelta(dx, dz, terrain.facingQuadrant());
        int up = position.getY() - terrain.originY();
        boolean inside = right >= -6 && right <= 6 && up >= -4 && up <= 4 && forward >= -6 && forward <= 6;
        if (!inside) return BlockInspection.outside(position.getX(), position.getY(), position.getZ(), right, up, forward);
        int index = LocalTerrainSnapshot.index(right, up, forward);
        int categoryOrdinal = terrain.categoryAt(index) & 0xFF;
        BlockSemanticCategory[] categories = BlockSemanticCategory.values();
        BlockSemanticCategory category = categoryOrdinal < categories.length ? categories[categoryOrdinal] : BlockSemanticCategory.UNKNOWN;
        return new BlockInspection(position.getX(), position.getY(), position.getZ(), right, up, forward,
            index, terrain.blockStateIdAt(index) & 0xFFFF, category, terrain.flagsAt(index),
            terrain.collisionHeightClassAt(index) & 0xFF, true);
    }

    public void cyclePage() { page = page.next(); }

    public void cycleEntity(ObservationSnapshot snapshot, int direction) {
        if (snapshot == null || snapshot.entities().count() == 0 || direction == 0) {
            selectedTrackingId = -1;
            return;
        }
        List<EntityObservation> entities = snapshot.entities().entities();
        int currentIndex = -1;
        for (int index = 0; index < entities.size(); index++) {
            if (entities.get(index).trackingId() == selectedTrackingId) {
                currentIndex = index;
                break;
            }
        }
        int next = currentIndex < 0 ? 0 : (currentIndex + (direction > 0 ? 1 : -1) + entities.size()) % entities.size();
        selectedTrackingId = entities.get(next).trackingId();
    }

    public static EntityObservation findEntity(ObservationSnapshot snapshot, int trackingId) {
        if (snapshot == null || trackingId <= 0) return null;
        for (EntityObservation entity : snapshot.entities().entities()) {
            if (entity.trackingId() == trackingId) return entity;
        }
        return null;
    }

    public InspectorPage page() { return page; }
    public int selectedTrackingId() { return selectedTrackingId; }
    public EntityObservation selectedEntity(ObservationSnapshot snapshot) { return findEntity(snapshot, selectedTrackingId); }
    public BlockInspection selectedBlock() { return selectedBlock; }
    public ObservationDiff latestDiff() { return latestDiff; }
}
