package dev.fivesaw.sawbot.forge.hud;

import dev.fivesaw.sawbot.common.observation.BlockSemanticCategory;
import dev.fivesaw.sawbot.common.observation.EgocentricTransform;
import dev.fivesaw.sawbot.common.observation.EntityObservation;
import dev.fivesaw.sawbot.common.observation.ItemCategory;
import dev.fivesaw.sawbot.common.observation.LandmarkObservation;
import dev.fivesaw.sawbot.common.observation.LocalTerrainSnapshot;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.common.observation.SelfState;
import dev.fivesaw.sawbot.forge.inspection.BlockInspection;
import dev.fivesaw.sawbot.forge.inspection.InspectorController;
import dev.fivesaw.sawbot.common.navigation.NavigationCell;
import dev.fivesaw.sawbot.common.bridging.BridgePlacementStep;
import dev.fivesaw.sawbot.forge.bridging.BridgingBodyController;
import dev.fivesaw.sawbot.forge.navigation.NavigationBodyController;
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
    private final NavigationBodyController navigationBody;
    private final BridgingBodyController bridgingBody;
    private final RollingTimingWindow renderTiming;

    public WorldDebugRenderer(Minecraft minecraft, SawBotStateController state,
                              InspectorController inspector, NavigationBodyController navigationBody,
                              BridgingBodyController bridgingBody, int timingWindowSize) {
        if (minecraft == null || state == null || inspector == null || navigationBody == null
            || bridgingBody == null) throw new IllegalArgumentException("renderer");
        this.minecraft = minecraft;
        this.state = state;
        this.inspector = inspector;
        this.navigationBody = navigationBody;
        this.bridgingBody = bridgingBody;
        this.renderTiming = new RollingTimingWindow(timingWindowSize);
    }

    public void render(ObservationSnapshot snapshot, float partialTicks) {
        if (snapshot == null || minecraft.theWorld == null || minecraft.thePlayer == null) return;
        if (!state.terrainOverlayVisible() && !state.collisionOverlayVisible()
            && !state.entityOverlayVisible() && !state.landmarkOverlayVisible()
            && !state.inspectorVisible()
            && bridgingBody.planSteps().isEmpty()) return;
        long start = System.nanoTime();
        boolean matrixPushed = false;
        try {
            RenderManager manager = minecraft.getRenderManager();
            restoreState();
            GlStateManager.pushMatrix();
            matrixPushed = true;
            GlStateManager.translate(-manager.viewerPosX, -manager.viewerPosY, -manager.viewerPosZ);
            if (state.terrainOverlayVisible()) renderTerrain(snapshot.localTerrain());
            if (state.collisionOverlayVisible()) renderCollision(snapshot);
            if (state.entityOverlayVisible()) renderEntities(snapshot, manager, partialTicks);
            if (state.landmarkOverlayVisible()) renderLandmarks(snapshot, manager);
            if (state.inspectorVisible() && !navigationBody.pathCells().isEmpty()) {
                renderNavigationPath();
            }
            if (!bridgingBody.planSteps().isEmpty()) renderBridgePlan();
            if (state.inspectorVisible()) renderSelectedBlock(inspector.selectedBlock());
            if (state.observationsFrozen()) renderFrozenAnchor(snapshot, manager);
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
        double eyeX = state.observationsFrozen() ? self.absoluteX()
            : interpolate(minecraft.thePlayer.lastTickPosX, minecraft.thePlayer.posX, partialTicks);
        double eyeY = state.observationsFrozen() ? self.absoluteY() + minecraft.thePlayer.getEyeHeight()
            : interpolate(minecraft.thePlayer.lastTickPosY, minecraft.thePlayer.posY, partialTicks)
                + minecraft.thePlayer.getEyeHeight();
        double eyeZ = state.observationsFrozen() ? self.absoluteZ()
            : interpolate(minecraft.thePlayer.lastTickPosZ, minecraft.thePlayer.posZ, partialTicks);
        int tracersDrawn = 0;
        for (EntityObservation entity : snapshot.entities().entities()) {
            double x = self.absoluteX() + EgocentricTransform.worldDx(entity.right(), entity.forward(), self.yawDegrees());
            double y = self.absoluteY() + entity.up();
            double z = self.absoluteZ() + EgocentricTransform.worldDz(entity.right(), entity.forward(), self.yawDegrees());
            double half = Math.max(0.1D, entity.width() * 0.5D);
            boolean isSelected = selected != null && selected.trackingId() == entity.trackingId();
            int visibilityRgb = EntityVisualStyle.visibilityRgb(entity);
            int red = EntityVisualStyle.red(visibilityRgb);
            int green = EntityVisualStyle.green(visibilityRgb);
            int blue = EntityVisualStyle.blue(visibilityRgb);
            drawBox(new AxisAlignedBB(x - half, y, z - half, x + half, y + entity.height(), z + half),
                red, green, blue, entity.occluded() ? 210 : 255);
            if (isSelected) {
                double accent = 0.035D;
                drawBox(new AxisAlignedBB(x - half - accent, y - accent, z - half - accent,
                    x + half + accent, y + entity.height() + accent, z + half + accent),
                    EntityVisualStyle.red(EntityVisualStyle.SELECTED_ACCENT_RGB),
                    EntityVisualStyle.green(EntityVisualStyle.SELECTED_ACCENT_RGB),
                    EntityVisualStyle.blue(EntityVisualStyle.SELECTED_ACCENT_RGB), 255);
            }
            if (state.entityTracersVisible() && tracersDrawn < MAX_ENTITY_TRACERS) {
                drawLine(eyeX, eyeY, eyeZ, x, y + entity.height() * 0.5D, z,
                    red, green, blue, entity.occluded() ? 210 : 255);
                tracersDrawn++;
            }
        }
        endLines();
        for (EntityObservation entity : snapshot.entities().entities()) {
            double x = self.absoluteX() + EgocentricTransform.worldDx(entity.right(), entity.forward(), self.yawDegrees());
            double y = self.absoluteY() + entity.up() + entity.height() + 0.35D;
            double z = self.absoluteZ() + EgocentricTransform.worldDz(entity.right(), entity.forward(), self.yawDegrees());
            boolean isSelected = selected != null && selected.trackingId() == entity.trackingId();
            String relation = entity.kind() == dev.fivesaw.sawbot.common.observation.EntityKind.PLAYER
                ? " " + entity.teamRelation() : "";
            String payload = entity.payloadItemCategory() == ItemCategory.EMPTY.ordinal()
                ? "" : " " + itemCategoryName(entity.payloadItemCategory());
            String label = (isSelected ? "> " : "") + "#" + entity.trackingId() + " " + entity.type() + " "
                + EntityVisualStyle.visibilityToken(entity) + relation + payload + " "
                + one(entity.distance()) + "m";
            renderLabel(label, x, y, z, manager, EntityVisualStyle.visibilityArgb(entity));
        }
    }

    private void renderLandmarks(ObservationSnapshot snapshot, RenderManager manager) {
        SelfState self = snapshot.selfState();
        beginLines(true, 1.5F);
        for (LandmarkObservation landmark : snapshot.landmarks().landmarks()) {
            double x = self.absoluteX() + EgocentricTransform.worldDx(landmark.right(), landmark.forward(), self.yawDegrees());
            double y = self.absoluteY() + landmark.up();
            double z = self.absoluteZ() + EgocentricTransform.worldDz(landmark.right(), landmark.forward(), self.yawDegrees());
            boolean learnedWaypoint = landmark.landmarkId() == dev.fivesaw.sawbot.forge.map.NavigationWaypointController.USER_WAYPOINT_ID;
            int red = learnedWaypoint ? 85 : 170;
            int green = learnedWaypoint ? 255 : 85;
            int blue = learnedWaypoint ? 170 : 255;
            drawLine(x, y, z, x, y + 2D, z, red, green, blue, 220);
            drawBox(new AxisAlignedBB(x - 0.15D, y - 0.15D, z - 0.15D,
                x + 0.15D, y + 0.15D, z + 0.15D), red, green, blue, 220);
        }
        endLines();
        for (LandmarkObservation landmark : snapshot.landmarks().landmarks()) {
            double x = self.absoluteX() + EgocentricTransform.worldDx(landmark.right(), landmark.forward(), self.yawDegrees());
            double y = self.absoluteY() + landmark.up() + 2.2D;
            double z = self.absoluteZ() + EgocentricTransform.worldDz(landmark.right(), landmark.forward(), self.yawDegrees());
            int colour = landmark.landmarkId() == dev.fivesaw.sawbot.forge.map.NavigationWaypointController.USER_WAYPOINT_ID
                ? 0xFF55FFAA : 0xFFAA55FF;
            renderLabel("WP#" + landmark.landmarkId() + " " + landmark.type(), x, y, z, manager, colour);
        }
    }


    private void renderNavigationPath() {
        java.util.List<NavigationCell> cells = navigationBody.pathCells();
        if (cells.isEmpty()) return;
        beginLines(true, 2.4F);
        int startIndex = Math.max(0, navigationBody.pathIndex() - 4);
        int endIndex = Math.min(cells.size(), startIndex + 96);
        for (int index = startIndex; index < endIndex; index++) {
            NavigationCell cell = cells.get(index);
            boolean current = index == navigationBody.pathIndex();
            boolean lookahead = index == navigationBody.lookaheadIndex();
            boolean provisional = navigationBody.provisionalPath();
            int red = current ? 255 : (lookahead ? 255 : (provisional ? 255 : 85));
            int green = current ? 255 : (lookahead ? 85 : (provisional ? 170 : 220));
            int blue = current ? 85 : (lookahead ? 255 : 255);
            double x = cell.centerX();
            double y = cell.centerY() + 0.08D;
            double z = cell.centerZ();
            drawBox(new AxisAlignedBB(x - 0.12D, y - 0.04D, z - 0.12D,
                x + 0.12D, y + 0.20D, z + 0.12D), red, green, blue, 220);
            if (index + 1 < endIndex) {
                NavigationCell next = cells.get(index + 1);
                drawLine(x, y + 0.08D, z, next.centerX(), next.centerY() + 0.16D,
                    next.centerZ(), red, green, blue, 230);
            }
        }
        endLines();
    }

    private void renderBridgePlan() {
        java.util.List<BridgePlacementStep> steps = bridgingBody.planSteps();
        if (steps.isEmpty()) return;
        beginLines(true, 2.8F);
        for (int index = 0; index < steps.size(); index++) {
            BridgePlacementStep step = steps.get(index);
            NavigationCell support = step.supportCell();
            boolean current = index == bridgingBody.stepIndex();
            int red = current ? 255 : 85;
            int green = current ? 170 : 255;
            int blue = current ? 55 : 170;
            double x = support.x();
            double y = support.y();
            double z = support.z();
            drawBox(new AxisAlignedBB(x + 0.04D, y + 0.04D, z + 0.04D,
                x + 0.96D, y + 0.96D, z + 0.96D), red, green, blue, 220);
            if (index + 1 < steps.size()) {
                NavigationCell next = steps.get(index + 1).supportCell();
                drawLine(x + 0.5D, y + 0.5D, z + 0.5D,
                    next.x() + 0.5D, next.y() + 0.5D, next.z() + 0.5D,
                    red, green, blue, 225);
            }
        }
        endLines();
    }

    private void renderSelectedBlock(BlockInspection block) {
        if (block == null) return;
        beginLines(true, 3.0F);
        drawBox(new AxisAlignedBB(block.worldX() - 0.01D, block.worldY() - 0.01D, block.worldZ() - 0.01D,
            block.worldX() + 1.01D, block.worldY() + 1.01D, block.worldZ() + 1.01D),
            255, 255, 85, 255);
        endLines();
    }


    private void renderFrozenAnchor(ObservationSnapshot snapshot, RenderManager manager) {
        SelfState self = snapshot.selfState();
        double y = self.absoluteY() + minecraft.thePlayer.getEyeHeight() + 0.55D;
        beginLines(true, 1.5F);
        drawBox(new AxisAlignedBB(self.absoluteX() - 0.18D, self.absoluteY(), self.absoluteZ() - 0.18D,
            self.absoluteX() + 0.18D, self.absoluteY() + 0.36D, self.absoluteZ() + 0.18D), 85, 255, 255, 220);
        endLines();
        renderLabel("FROZEN SNAPSHOT #" + snapshot.sequenceNumber(), self.absoluteX(), y, self.absoluteZ(), manager, 0xFF55FFFF);
    }

    private static String itemCategoryName(int ordinal) {
        ItemCategory[] values = ItemCategory.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal].name() : "INVALID_ITEM";
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
        GlStateManager.resetColor();
        GL11.glColor4f(1F, 1F, 1F, 1F);
        font.drawStringWithShadow(text, -font.getStringWidth(text) / 2, 0, color);
        GlStateManager.resetColor();
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GlStateManager.popMatrix();
    }

    private static void restoreState() {
        GL11.glLineWidth(1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.resetColor();
        GL11.glColor4f(1F, 1F, 1F, 1F);
        GlStateManager.color(1F, 1F, 1F, 1F);
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
