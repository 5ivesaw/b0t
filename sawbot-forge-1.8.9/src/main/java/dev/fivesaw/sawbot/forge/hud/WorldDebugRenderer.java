package dev.fivesaw.sawbot.forge.hud;

import dev.fivesaw.sawbot.common.observation.BlockSemanticCategory;
import dev.fivesaw.sawbot.common.observation.EgocentricTransform;
import dev.fivesaw.sawbot.common.observation.EntityObservation;
import dev.fivesaw.sawbot.common.observation.LandmarkObservation;
import dev.fivesaw.sawbot.common.observation.LocalTerrainSnapshot;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.common.observation.SelfState;
import dev.fivesaw.sawbot.forge.inspection.BlockInspection;
import dev.fivesaw.sawbot.forge.inspection.InspectorController;
import dev.fivesaw.sawbot.forge.performance.RollingTimingWindow;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

/** Bounded Phase 2 world-space overlays. Every overlay is individually toggleable. */
public final class WorldDebugRenderer {
    private static final int MAX_TERRAIN_BOXES = 256;
    private static final int MAX_COLLISION_BOXES = 256;
    private static final int MAX_ENTITY_TRACERS = 16;
    private static final double EPSILON = 0.002D;
    private final Minecraft minecraft;
    private final SawBotStateController state;
    private final InspectorController inspector;
    private final RollingTimingWindow renderTiming;

    public WorldDebugRenderer(Minecraft minecraft, SawBotStateController state,
                              InspectorController inspector, int timingWindowSize) {
        if (minecraft == null || state == null || inspector == null) throw new IllegalArgumentException("renderer");
        this.minecraft = minecraft;
        this.state = state;
        this.inspector = inspector;
        this.renderTiming = new RollingTimingWindow(timingWindowSize);
    }

    public void render(ObservationSnapshot snapshot, float partialTicks) {
        if (snapshot == null || minecraft.theWorld == null || minecraft.thePlayer == null) return;
        if (!state.terrainOverlayVisible() && !state.collisionOverlayVisible()
            && !state.entityOverlayVisible() && !state.landmarkOverlayVisible()
            && !state.inspectorVisible()) return;
        long start = System.nanoTime();
        boolean matrixPushed = false;
        try {
            RenderManager manager = minecraft.getRenderManager();
            GlStateManager.pushMatrix();
            matrixPushed = true;
            GlStateManager.translate(-manager.viewerPosX, -manager.viewerPosY, -manager.viewerPosZ);
            if (state.terrainOverlayVisible()) renderTerrain(snapshot.localTerrain());
            if (state.collisionOverlayVisible()) renderCollision(snapshot);
            if (state.entityOverlayVisible()) renderEntities(snapshot, manager, partialTicks);
            if (state.landmarkOverlayVisible()) renderLandmarks(snapshot, manager);
            renderSelectedBlock(inspector.selectedBlock());
        } finally {
            if (matrixPushed) GlStateManager.popMatrix();
            restoreState();
            renderTiming.add(Math.max(0L, System.nanoTime() - start));
        }
    }

    private void renderTerrain(LocalTerrainSnapshot terrain) {
        beginLines(false, 1.0F);
        drawBox(new AxisAlignedBB(terrain.originX() - 6D, terrain.originY() - 4D, terrain.originZ() - 6D,
            terrain.originX() + 7D, terrain.originY() + 5D, terrain.originZ() + 7D), 85, 255, 255, 180);
        int drawn = 0;
        for (int up = -4; up <= 4 && drawn < MAX_TERRAIN_BOXES; up++) {
            for (int forward = -6; forward <= 6 && drawn < MAX_TERRAIN_BOXES; forward++) {
                for (int right = -6; right <= 6 && drawn < MAX_TERRAIN_BOXES; right++) {
                    int index = LocalTerrainSnapshot.index(right, up, forward);
                    int categoryOrdinal = terrain.categoryAt(index) & 0xFF;
                    BlockSemanticCategory[] values = BlockSemanticCategory.values();
                    BlockSemanticCategory category = categoryOrdinal < values.length ? values[categoryOrdinal] : BlockSemanticCategory.UNKNOWN;
                    if (category == BlockSemanticCategory.AIR || category == BlockSemanticCategory.UNKNOWN) continue;
                    int worldX = terrain.originX() + EgocentricTransform.worldDx(right, forward, terrain.facingQuadrant());
                    int worldY = terrain.originY() + up;
                    int worldZ = terrain.originZ() + EgocentricTransform.worldDz(right, forward, terrain.facingQuadrant());
                    int[] color = categoryColor(category);
                    double height = collisionHeight(terrain.collisionHeightClassAt(index) & 0xFF);
                    drawBox(new AxisAlignedBB(worldX + EPSILON, worldY + EPSILON, worldZ + EPSILON,
                        worldX + 1D - EPSILON, worldY + Math.max(0.08D, height) - EPSILON,
                        worldZ + 1D - EPSILON), color[0], color[1], color[2], 105);
                    drawn++;
                }
            }
        }
        endLines();
    }

    private void renderCollision(ObservationSnapshot snapshot) {
        LocalTerrainSnapshot terrain = snapshot.localTerrain();
        beginLines(false, 1.4F);
        int drawn = 0;
        for (int up = -4; up <= 4 && drawn < MAX_COLLISION_BOXES; up++) {
            for (int forward = -6; forward <= 6 && drawn < MAX_COLLISION_BOXES; forward++) {
                for (int right = -6; right <= 6 && drawn < MAX_COLLISION_BOXES; right++) {
                    int index = LocalTerrainSnapshot.index(right, up, forward);
                    int collisionClass = terrain.collisionHeightClassAt(index) & 0xFF;
                    if (collisionClass == 0) continue;
                    int worldX = terrain.originX() + EgocentricTransform.worldDx(right, forward, terrain.facingQuadrant());
                    int worldY = terrain.originY() + up;
                    int worldZ = terrain.originZ() + EgocentricTransform.worldDz(right, forward, terrain.facingQuadrant());
                    short flags = terrain.flagsAt(index);
                    boolean safe = (flags & LocalTerrainSnapshot.FLAG_SAFE_SUPPORT) != 0;
                    drawBox(new AxisAlignedBB(worldX + 0.01D, worldY + 0.01D, worldZ + 0.01D,
                        worldX + 0.99D, worldY + collisionHeight(collisionClass) - 0.01D, worldZ + 0.99D),
                        safe ? 85 : 255, safe ? 255 : 170, safe ? 85 : 0, 150);
                    drawn++;
                }
            }
        }
        renderSupportSamples(snapshot.selfState());
        endLines();
    }

    private void renderSupportSamples(SelfState self) {
        double yaw = Math.toRadians(self.yawDegrees());
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double[] offsets = {-0.25D, 0D, 0.25D};
        float[] distances = {self.supportDistanceLeft(), self.supportDistanceCenter(), self.supportDistanceRight()};
        for (int i = 0; i < offsets.length; i++) {
            double x = self.absoluteX() + rightX * offsets[i];
            double z = self.absoluteZ() + rightZ * offsets[i];
            double top = self.absoluteY() + 0.05D;
            double bottom = self.absoluteY() - Math.min(64D, distances[i]);
            drawLine(x, top, z, x, bottom, z, distances[i] >= 8F ? 255 : 85,
                distances[i] >= 8F ? 85 : 255, 85, 220);
        }
    }

    private void renderEntities(ObservationSnapshot snapshot, RenderManager manager, float partialTicks) {
        beginLines(true, 2.0F);
        SelfState self = snapshot.selfState();
        EntityObservation selected = inspector.selectedEntity(snapshot);
        double eyeX = interpolate(minecraft.thePlayer.lastTickPosX, minecraft.thePlayer.posX, partialTicks);
        double eyeY = interpolate(minecraft.thePlayer.lastTickPosY, minecraft.thePlayer.posY, partialTicks)
            + minecraft.thePlayer.getEyeHeight();
        double eyeZ = interpolate(minecraft.thePlayer.lastTickPosZ, minecraft.thePlayer.posZ, partialTicks);
        int tracersDrawn = 0;
        for (EntityObservation entity : snapshot.entities().entities()) {
            double x = self.absoluteX() + EgocentricTransform.worldDx(entity.right(), entity.forward(), self.yawDegrees());
            double y = self.absoluteY() + entity.up();
            double z = self.absoluteZ() + EgocentricTransform.worldDz(entity.right(), entity.forward(), self.yawDegrees());
            double half = Math.max(0.1D, entity.width() * 0.5D);
            int[] color = entityColor(entity, selected != null && selected.trackingId() == entity.trackingId());
            drawBox(new AxisAlignedBB(x - half, y, z - half, x + half, y + entity.height(), z + half),
                color[0], color[1], color[2], entity.occluded() ? 145 : 230);
            if (state.entityTracersVisible() && tracersDrawn < MAX_ENTITY_TRACERS) {
                drawLine(eyeX, eyeY, eyeZ, x, y + entity.height() * 0.5D, z,
                    color[0], color[1], color[2], entity.lineOfSight() ? 180 : 80);
                tracersDrawn++;
            }
        }
        endLines();
        for (EntityObservation entity : snapshot.entities().entities()) {
            double x = self.absoluteX() + EgocentricTransform.worldDx(entity.right(), entity.forward(), self.yawDegrees());
            double y = self.absoluteY() + entity.up() + entity.height() + 0.35D;
            double z = self.absoluteZ() + EgocentricTransform.worldDz(entity.right(), entity.forward(), self.yawDegrees());
            String label = "#" + entity.trackingId() + " " + entity.kind() + " "
                + (entity.lineOfSight() ? "LOS" : "OCC") + " " + one(entity.distance()) + "m";
            renderLabel(label, x, y, z, manager, selected != null && selected.trackingId() == entity.trackingId() ? 0xFFFFFF55 : 0xFFFFFFFF);
        }
    }

    private void renderLandmarks(ObservationSnapshot snapshot, RenderManager manager) {
        SelfState self = snapshot.selfState();
        beginLines(true, 1.5F);
        for (LandmarkObservation landmark : snapshot.landmarks().landmarks()) {
            double x = self.absoluteX() + EgocentricTransform.worldDx(landmark.right(), landmark.forward(), self.yawDegrees());
            double y = self.absoluteY() + landmark.up();
            double z = self.absoluteZ() + EgocentricTransform.worldDz(landmark.right(), landmark.forward(), self.yawDegrees());
            drawLine(x, y, z, x, y + 2D, z, 170, 85, 255, 220);
            drawBox(new AxisAlignedBB(x - 0.15D, y - 0.15D, z - 0.15D,
                x + 0.15D, y + 0.15D, z + 0.15D), 170, 85, 255, 220);
        }
        endLines();
        for (LandmarkObservation landmark : snapshot.landmarks().landmarks()) {
            double x = self.absoluteX() + EgocentricTransform.worldDx(landmark.right(), landmark.forward(), self.yawDegrees());
            double y = self.absoluteY() + landmark.up() + 2.2D;
            double z = self.absoluteZ() + EgocentricTransform.worldDz(landmark.right(), landmark.forward(), self.yawDegrees());
            renderLabel("WP#" + landmark.landmarkId() + " " + landmark.type(), x, y, z, manager, 0xFFAA55FF);
        }
    }

    private void renderSelectedBlock(BlockInspection block) {
        if (block == null) return;
        beginLines(true, 3.0F);
        drawBox(new AxisAlignedBB(block.worldX() - 0.01D, block.worldY() - 0.01D, block.worldZ() - 0.01D,
            block.worldX() + 1.01D, block.worldY() + 1.01D, block.worldZ() + 1.01D),
            block.insideTensor() ? 255 : 255, block.insideTensor() ? 255 : 85, 85, 255);
        endLines();
    }

    private static int[] categoryColor(BlockSemanticCategory category) {
        switch (category) {
            case HAZARD: return new int[]{255, 55, 55};
            case LIQUID: return new int[]{85, 145, 255};
            case CLIMBABLE: return new int[]{85, 255, 85};
            case BED: return new int[]{255, 85, 170};
            case CONTAINER: return new int[]{255, 170, 55};
            case INTERACTABLE: return new int[]{255, 255, 85};
            case PARTIAL: return new int[]{170, 170, 255};
            case SOLID: return new int[]{170, 170, 170};
            case PLANT: return new int[]{85, 210, 85};
            default: return new int[]{120, 120, 120};
        }
    }

    private static int[] entityColor(EntityObservation entity, boolean selected) {
        if (selected) return new int[]{255, 255, 85};
        switch (entity.teamRelation()) {
            case ENEMY: return new int[]{255, 75, 75};
            case TEAMMATE: return new int[]{85, 255, 85};
            case NEUTRAL: return new int[]{85, 200, 255};
            default: return entity.occluded() ? new int[]{170, 85, 255} : new int[]{220, 220, 220};
        }
    }

    private static double collisionHeight(int collisionClass) {
        switch (collisionClass) {
            case 1: return 0.25D;
            case 2: return 0.5D;
            case 3: return 0.75D;
            case 4: return 1D;
            case 5: return 1D;
            case 6: return 1D;
            default: return 0.08D;
        }
    }

    private static void beginLines(boolean xray, float width) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        if (xray) GlStateManager.disableDepth(); else GlStateManager.enableDepth();
        GL11.glLineWidth(width);
        Tessellator tessellator = Tessellator.getInstance();
        tessellator.getWorldRenderer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
    }

    private static void endLines() { Tessellator.getInstance().draw(); }

    private static void drawBox(AxisAlignedBB box, int r, int g, int b, int a) {
        drawLine(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, r,g,b,a);
        drawLine(box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r,g,b,a);
        drawLine(box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r,g,b,a);
        drawLine(box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, r,g,b,a);
        drawLine(box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, r,g,b,a);
        drawLine(box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r,g,b,a);
        drawLine(box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, r,g,b,a);
        drawLine(box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, r,g,b,a);
        drawLine(box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, r,g,b,a);
        drawLine(box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, r,g,b,a);
        drawLine(box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, r,g,b,a);
        drawLine(box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, r,g,b,a);
    }

    private static void drawLine(double x1,double y1,double z1,double x2,double y2,double z2,
                                 int r,int g,int b,int a) {
        WorldRenderer renderer = Tessellator.getInstance().getWorldRenderer();
        renderer.pos(x1,y1,z1).color(r,g,b,a).endVertex();
        renderer.pos(x2,y2,z2).color(r,g,b,a).endVertex();
    }

    private void renderLabel(String text, double x, double y, double z, RenderManager manager, int color) {
        FontRenderer font = minecraft.fontRendererObj;
        if (font == null) return;
        float scale = 0.018F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-manager.playerViewY, 0F, 1F, 0F);
        GlStateManager.rotate(manager.playerViewX, 1F, 0F, 0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        font.drawStringWithShadow(text, -font.getStringWidth(text) / 2, 0, color);
        GlStateManager.popMatrix();
    }

    private static void restoreState() {
        GL11.glLineWidth(1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
    }

    private static double interpolate(double previous, double current, float partialTicks) {
        double t = Math.max(0D, Math.min(1D, partialTicks));
        return previous + (current - previous) * t;
    }

    private static String one(float value) {
        long scaled = Math.round(Math.abs(value) * 10F);
        return (value < 0F ? "-" : "") + (scaled / 10L) + "." + (scaled % 10L);
    }

    public long averageRenderNanos() { return renderTiming.averageNanos(); }
    public long maximumRenderNanos() { return renderTiming.maximumNanos(); }
}
