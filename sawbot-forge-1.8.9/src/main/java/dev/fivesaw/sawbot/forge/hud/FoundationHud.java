package dev.fivesaw.sawbot.forge.hud;

import dev.fivesaw.sawbot.common.events.ObservationEvent;
import dev.fivesaw.sawbot.common.observation.BlockSemanticCategory;
import dev.fivesaw.sawbot.common.observation.EntityObservation;
import dev.fivesaw.sawbot.common.observation.InventorySnapshot;
import dev.fivesaw.sawbot.common.observation.ItemSlotObservation;
import dev.fivesaw.sawbot.common.observation.LocalTerrainSnapshot;
import dev.fivesaw.sawbot.common.observation.ObservationDiff;
import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.common.observation.SelfState;
import dev.fivesaw.sawbot.common.observation.SensorTimings;
import dev.fivesaw.sawbot.forge.client.SawBotKeyBindings;
import dev.fivesaw.sawbot.forge.inspection.BlockInspection;
import dev.fivesaw.sawbot.forge.inspection.InspectorController;
import dev.fivesaw.sawbot.forge.inspection.InspectorPage;
import dev.fivesaw.sawbot.forge.inspection.SnapshotExportService;
import dev.fivesaw.sawbot.forge.performance.RollingTimingWindow;
import dev.fivesaw.sawbot.forge.safety.SawBotStateController;
import dev.fivesaw.sawbot.forge.sensors.ObservationPipeline;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/** Premium compact HUD and progressive Phase 2 inspector workspace. */
public final class FoundationHud {
    private static final int STATUS_HEIGHT = 58;
    private static final int PANEL_HEIGHT = 270;
    private static final int PANEL_MIN_HEIGHT = 176;
    private static final long TOAST_VISIBLE_NANOS = 2_600_000_000L;

    private final Minecraft minecraft;
    private final SawBotStateController state;
    private final RollingTimingWindow tickTiming;
    private final ObservationPipeline observations;
    private final InspectorController inspector;
    private final SnapshotExportService exports;
    private final WorldDebugRenderer worldRenderer;
    private final SawBotKeyBindings keys;
    private final boolean animationsEnabled;
    private final GlassUi ui;
    private final RollingTimingWindow hudTiming;
    private final MotionValue inspectorReveal = new MotionValue();
    private final MotionValue toastReveal = new MotionValue();
    private String lastNotice = "";
    private long noticeChangedNanos;

    public FoundationHud(Minecraft minecraft, SawBotStateController state,
                         RollingTimingWindow tickTiming, ObservationPipeline observations,
                         InspectorController inspector, SnapshotExportService exports,
                         WorldDebugRenderer worldRenderer, SawBotKeyBindings keys,
                         int timingWindowSize, boolean animationsEnabled) {
        if (minecraft == null || state == null || tickTiming == null || observations == null
            || inspector == null || exports == null || worldRenderer == null || keys == null) {
            throw new IllegalArgumentException("hud dependencies");
        }
        this.minecraft = minecraft;
        this.state = state;
        this.tickTiming = tickTiming;
        this.observations = observations;
        this.inspector = inspector;
        this.exports = exports;
        this.worldRenderer = worldRenderer;
        this.keys = keys;
        this.animationsEnabled = animationsEnabled;
        this.ui = new GlassUi(minecraft);
        this.hudTiming = new RollingTimingWindow(timingWindowSize);
    }

    public void render(long clientTick) {
        if (minecraft.fontRendererObj == null) return;
        long renderStart = System.nanoTime();
        try {
            long now = renderStart;
            ScaledResolution resolution = new ScaledResolution(minecraft);
            int screenWidth = resolution.getScaledWidth();
            int screenHeight = resolution.getScaledHeight();
            ObservationSnapshot snapshot = observations.latest();
            updateNotice(now);

            int panelWidth = Math.max(280, Math.min(366, screenWidth - 16));
            int statusWidth = Math.min(286, panelWidth);
            float reveal = inspectorReveal.update(state.inspectorVisible(), animationsEnabled, now);
            int expectedPanelHeight = reveal > 0.01F ? PANEL_HEIGHT + 8 : 0;
            int totalHeight = STATUS_HEIGHT + expectedPanelHeight;
            int x = 8;
            int y = 8;
            if (minecraft.gameSettings.showDebugInfo && reveal <= 0.01F) {
                y = Math.max(8, screenHeight - STATUS_HEIGHT - 8);
            } else if (totalHeight > screenHeight - 16) {
                y = 8;
            }

            drawStatusIsland(snapshot, clientTick, x, y, statusWidth, now);
            if (reveal > 0.01F) {
                int panelY = y + STATUS_HEIGHT + 8 - Math.round((1F - ease(reveal)) * 6F);
                int available = Math.max(PANEL_MIN_HEIGHT, screenHeight - panelY - 8);
                int height = Math.min(PANEL_HEIGHT, available);
                drawInspector(snapshot, x, panelY, panelWidth, height, ease(reveal));
            }
            drawToast(screenWidth, y, statusWidth, now);
        } finally {
            hudTiming.add(Math.max(0L, System.nanoTime() - renderStart));
        }
    }

    private void drawStatusIsland(ObservationSnapshot snapshot, long clientTick, int x, int y,
                                  int width, long now) {
        int stateColor = state.observationsFrozen() ? UiTheme.CYAN : state.isEnabled() ? UiTheme.WARNING : UiTheme.SAFE;
        ui.panel(x, y, width, STATUS_HEIGHT, UiTheme.RADIUS_PANEL, UiTheme.GLASS, 1F);
        ui.roundedRect(x, y + 9, 3, STATUS_HEIGHT - 18, 2, stateColor);
        ui.robotMark(x + 10, y + 10, 34, stateColor, 1F);
        ui.scaledText("SawBot", x + 52, y + 9, UiTheme.TEXT_PRIMARY, 1F, 1.12F);
        ui.text("PHASE 2  /  INTERNAL EYES", x + 52, y + 25, UiTheme.TEXT_TERTIARY, 1F);

        String mode = state.observationsFrozen() ? "FROZEN" : state.isEnabled() ? "ENABLED" : "SAFE";
        int pillWidth = Math.max(48, ui.textWidth(mode) + 14);
        ui.pill(mode, x + width - pillWidth - 9, y + 9, pillWidth, 17, stateColor, true, 1F);

        if (snapshot == null) {
            ui.dot(x + 54, y + 45, 2, UiTheme.WARNING, 1F);
            ui.text("Waiting for a world snapshot", x + 61, y + 41, UiTheme.TEXT_SECONDARY, 1F);
            return;
        }

        long age = observations.snapshotAgeMillis();
        int freshnessColor = freshnessColor(age);
        String observation = "OBS #" + snapshot.sequenceNumber();
        ui.text(observation, x + 52, y + 41, UiTheme.TEXT_PRIMARY, 1F);
        int ageX = x + 52 + ui.textWidth(observation) + 8;
        ui.text(age + " ms", ageX, y + 41, freshnessColor, 1F);
        int barX = ageX + ui.textWidth(age + " ms") + 8;
        int barWidth = Math.max(22, x + width - 10 - barX);
        ui.progress(barX, y + 44, barWidth, 3, 1F - Math.min(1F, age / 1000F), freshnessColor, 1F);

        String sensorCost = micros(snapshot.sensorTimings().totalNanos()) + " us";
        String tickText = "T" + clientTick;
        int metaRight = x + width - 10;
        ui.textRight(sensorCost, metaRight, y + 29, UiTheme.TEXT_SECONDARY, 1F);
        ui.textRight(tickText, metaRight - ui.textWidth(sensorCost) - 10, y + 29, UiTheme.TEXT_TERTIARY, 1F);
    }

    private void drawInspector(ObservationSnapshot snapshot, int x, int y, int width, int height, float opacity) {
        ui.panel(x, y, width, height, UiTheme.RADIUS_PANEL, UiTheme.GLASS, opacity);
        int innerX = x + 10;
        int innerWidth = width - 20;
        InspectorPage page = inspector.page();
        String title = pretty(page.name());
        ui.text(title, innerX, y + 11, UiTheme.TEXT_PRIMARY, opacity);
        ui.text("INSPECTOR", innerX, y + 23, UiTheme.TEXT_TERTIARY, opacity);

        String pageCounter = (page.ordinal() + 1) + "/" + InspectorPage.values().length;
        int nextWidth = ui.textWidth(KeyLabel.of(keys.cycleInspectorPage) + "  NEXT") + 14;
        ui.pill(KeyLabel.of(keys.cycleInspectorPage) + "  NEXT", x + width - nextWidth - 10, y + 9,
            nextWidth, 18, UiTheme.ACCENT, false, opacity);
        ui.textRight(pageCounter, x + width - 12, y + 29, UiTheme.TEXT_TERTIARY, opacity);
        drawPageDots(innerX + 86, y + 26, page.ordinal(), InspectorPage.values().length, opacity);
        ui.divider(innerX, y + 39, innerWidth, opacity);

        int overlayY = y + height - 58;
        int footerY = y + height - 27;
        int contentY = y + 47;
        int contentBottom = overlayY - 7;
        if (snapshot == null) {
            drawEmptyState(innerX, contentY, innerWidth, contentBottom - contentY,
                "No observation", "Join a local world to populate the inspector.", UiTheme.WARNING, opacity);
        } else {
            renderPage(snapshot, innerX, contentY, innerWidth, contentBottom, opacity);
        }
        drawOverlayControls(innerX, overlayY, innerWidth, opacity);
        drawFooter(innerX, footerY, innerWidth, opacity);
    }

    private void renderPage(ObservationSnapshot snapshot, int x, int y, int width, int bottom, float opacity) {
        switch (inspector.page()) {
            case BODY: renderBody(snapshot, x, y, width, bottom, opacity); break;
            case TERRAIN: renderTerrain(snapshot, x, y, width, bottom, opacity); break;
            case ENTITIES: renderEntities(snapshot, x, y, width, bottom, opacity); break;
            case INVENTORY: renderInventory(snapshot, x, y, width, bottom, opacity); break;
            case EVENTS: renderEvents(snapshot, x, y, width, bottom, opacity); break;
            case DIFFERENCE: renderDifference(snapshot, x, y, width, bottom, opacity); break;
            case SYSTEM: renderSystem(snapshot, x, y, width, bottom, opacity); break;
            case SUMMARY:
            default: renderSummary(snapshot, x, y, width, bottom, opacity); break;
        }
    }

    private void renderSummary(ObservationSnapshot snapshot, int x, int y, int width, int bottom, float opacity) {
        SelfState self = snapshot.selfState();
        int gap = 6;
        int cardWidth = (width - gap * 2) / 3;
        drawMetricCard(x, y, cardWidth, 42, "HEALTH", one(self.health()), one(self.absorption()) + " absorption",
            self.health() <= 6F ? UiTheme.DANGER : UiTheme.SAFE, opacity);
        drawMetricCard(x + cardWidth + gap, y, cardWidth, 42, "ENTITIES",
            Integer.toString(snapshot.entities().count()), snapshot.entities().droppedCount() + " dropped",
            UiTheme.ACCENT, opacity);
        drawMetricCard(x + (cardWidth + gap) * 2, y, width - (cardWidth + gap) * 2, 42, "SENSOR",
            micros(snapshot.sensorTimings().totalNanos()) + " us", observations.snapshotAgeMillis() + " ms old",
            freshnessColor(observations.snapshotAgeMillis()), opacity);

        int rowY = y + 49;
        if (rowY + 46 < bottom) {
            int leftWidth = (width - gap) / 2;
            EntityObservation entity = inspector.selectedEntity(snapshot);
            ui.card(x, rowY, leftWidth, 48, opacity);
            ui.text("SELECTED ENTITY", x + 8, rowY + 7, UiTheme.TEXT_TERTIARY, opacity);
            if (entity == null) {
                ui.text("No tracked target", x + 8, rowY + 22, UiTheme.TEXT_SECONDARY, opacity);
            } else {
                int color = EntityVisualStyle.visibilityArgb(entity);
                ui.dot(x + 9, rowY + 28, 2, color, opacity);
                ui.text(ui.fit("#" + entity.trackingId() + "  " + entity.kind() + "  "
                    + EntityVisualStyle.visibilityToken(entity), leftWidth - 25), x + 16, rowY + 22, color, opacity);
                ui.text(one(entity.distance()) + "m  /  " + entity.teamRelation(), x + 8, rowY + 35,
                    UiTheme.TEXT_SECONDARY, opacity);
            }

            int rightX = x + leftWidth + gap;
            int rightWidth = width - leftWidth - gap;
            BlockInspection block = inspector.selectedBlock();
            ui.card(rightX, rowY, rightWidth, 48, opacity);
            ui.text("SELECTED BLOCK", rightX + 8, rowY + 7, UiTheme.TEXT_TERTIARY, opacity);
            if (block == null) {
                ui.text("Aim at a block", rightX + 8, rowY + 22, UiTheme.TEXT_SECONDARY, opacity);
                ui.text("Selection is automatic", rightX + 8, rowY + 35, UiTheme.TEXT_TERTIARY, opacity);
            } else {
                ui.text(ui.fit(block.category() + "  /  cell " + block.terrainIndex(), rightWidth - 16),
                    rightX + 8, rowY + 22, UiTheme.YELLOW, opacity);
                ui.text(block.worldX() + ", " + block.worldY() + ", " + block.worldZ(), rightX + 8,
                    rowY + 35, UiTheme.TEXT_SECONDARY, opacity);
            }
        }

        int finalY = rowY + 55;
        if (finalY + 32 < bottom) {
            ui.card(x, finalY, width, 34, opacity);
            ui.text("SUPPORT", x + 8, finalY + 6, UiTheme.TEXT_TERTIARY, opacity);
            ui.text("L " + one(self.supportDistanceLeft()) + "   C " + one(self.supportDistanceCenter())
                + "   R " + one(self.supportDistanceRight()), x + 8, finalY + 19, UiTheme.CYAN, opacity);
            String resources = "Fe " + snapshot.inventory().iron() + "   Au " + snapshot.inventory().gold()
                + "   D " + snapshot.inventory().diamonds() + "   E " + snapshot.inventory().emeralds()
                + "   W " + snapshot.inventory().wool();
            ui.textRight(resources, x + width - 8, finalY + 19, UiTheme.TEXT_SECONDARY, opacity);
        }
    }

    private void renderBody(ObservationSnapshot snapshot, int x, int y, int width, int bottom, float opacity) {
        SelfState s = snapshot.selfState();
        int row = beginDataCard(x, y, width, bottom, "BODY STATE", "Exact client-thread player state", UiTheme.ACCENT, opacity);
        row = dataRow(x, row, width, bottom, "Health", one(s.health()) + "  +" + one(s.absorption()) + " absorb", s.health() <= 6F ? UiTheme.DANGER : UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Position", three(s.absoluteX()) + "  " + three(s.absoluteY()) + "  " + three(s.absoluteZ()), UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Velocity R/U/F", three(s.velocityRight()) + " / " + three(s.velocityUp()) + " / " + three(s.velocityForward()), UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Acceleration", three(s.accelerationRight()) + " / " + three(s.accelerationUp()) + " / " + three(s.accelerationForward()), UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "View", one(s.yawDegrees()) + " yaw  /  " + one(s.pitchDegrees()) + " pitch", UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Contact", flag(s.onGround(), "GROUND") + "  " + flag(s.horizontalCollision(), "H-COL") + "  " + flag(s.verticalCollision(), "V-COL"), UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Motion", flag(s.sprinting(), "SPRINT") + "  " + flag(s.sneaking(), "SNEAK") + "  air " + s.airborneTicks(), UiTheme.TEXT_PRIMARY, opacity);
        dataRow(x, row, width, bottom, "Support L/C/R", one(s.supportDistanceLeft()) + " / " + one(s.supportDistanceCenter()) + " / " + one(s.supportDistanceRight()) + "  void " + one(s.distanceToVoid()), UiTheme.CYAN, opacity);
    }

    private void renderTerrain(ObservationSnapshot snapshot, int x, int y, int width, int bottom, float opacity) {
        LocalTerrainSnapshot terrain = snapshot.localTerrain();
        int[] counts = new int[BlockSemanticCategory.values().length];
        for (byte raw : terrain.categories()) {
            int index = raw & 0xFF;
            if (index < counts.length) counts[index]++;
        }
        int row = beginDataCard(x, y, width, bottom, "LOCAL TERRAIN", "13 x 9 x 13 egocentric tensor", UiTheme.CYAN, opacity);
        row = dataRow(x, row, width, bottom, "Origin", terrain.originX() + ", " + terrain.originY() + ", " + terrain.originZ() + "  Q" + terrain.facingQuadrant(), UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Full / Partial", counts[BlockSemanticCategory.SOLID.ordinal()] + " / " + counts[BlockSemanticCategory.PARTIAL.ordinal()], UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Liquid / Hazard", counts[BlockSemanticCategory.LIQUID.ordinal()] + " / " + counts[BlockSemanticCategory.HAZARD.ordinal()], UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Changed", terrain.changedCellCount() + " cells  /  " + snapshot.midRangeMap().rowsUpdatedThisTick() + " map rows", UiTheme.TEXT_PRIMARY, opacity);
        BlockInspection block = inspector.selectedBlock();
        if (block == null) {
            dataRow(x, row, width, bottom, "Crosshair", "Aim at a block to decode its tensor cell", UiTheme.TEXT_SECONDARY, opacity);
        } else {
            row = dataRow(x, row, width, bottom, "Selected", block.category() + "  state " + block.blockStateId(), UiTheme.YELLOW, opacity);
            row = dataRow(x, row, width, bottom, "World", block.worldX() + ", " + block.worldY() + ", " + block.worldZ(), UiTheme.TEXT_PRIMARY, opacity);
            row = dataRow(x, row, width, bottom, "Tensor R/U/F", block.rightOffset() + " / " + block.upOffset() + " / " + block.forwardOffset() + "  index " + block.terrainIndex(), UiTheme.TEXT_PRIMARY, opacity);
            dataRow(x, row, width, bottom, "Flags / collision", "0x" + Integer.toHexString(block.flags() & 0xFFFF) + "  /  class " + block.collisionHeightClass(), UiTheme.TEXT_PRIMARY, opacity);
        }
    }

    private void renderEntities(ObservationSnapshot snapshot, int x, int y, int width, int bottom, float opacity) {
        EntityObservation e = inspector.selectedEntity(snapshot);
        if (e == null) {
            drawEmptyState(x, y, width, bottom - y, "No tracked entities",
                "Spawn a mob or local player, then use " + KeyLabel.of(keys.previousEntity) + " / "
                    + KeyLabel.of(keys.nextEntity) + " to select.", UiTheme.PURPLE, opacity);
            return;
        }
        int color = EntityVisualStyle.visibilityArgb(e);
        int row = beginDataCard(x, y, width, bottom, "ENTITY #" + e.trackingId(),
            e.kind() + "  /  " + EntityVisualStyle.visibilityToken(e) + "  /  " + e.teamRelation(), color, opacity);
        row = dataRow(x, row, width, bottom, "Minecraft ID", Integer.toString(e.minecraftEntityId()), UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Distance", one(e.distance()) + " m  /  confidence " + one(e.trackingConfidence()), color, opacity);
        row = dataRow(x, row, width, bottom, "Relative R/U/F", one(e.right()) + " / " + one(e.up()) + " / " + one(e.forward()), UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Velocity", three(e.velocityRight()) + " / " + three(e.velocityUp()) + " / " + three(e.velocityForward()), UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Health / armour", one(e.health()) + " / " + one(e.armour()), UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Held category", Integer.toString(e.heldItemCategory()) + "  /  hurt " + e.hurtTimerTicks(), UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Visibility", flag(e.lineOfSight(), "LOS") + "  " + flag(e.occluded(), "OCC") + "  " + flag(e.attackable(), "ATTACKABLE"), color, opacity);
        dataRow(x, row, width, bottom, "Movement", flag(e.onGround(), "GROUND") + "  " + flag(e.sprinting(), "SPRINT") + "  " + flag(e.sneaking(), "SNEAK"), UiTheme.TEXT_PRIMARY, opacity);
    }

    private void renderInventory(ObservationSnapshot snapshot, int x, int y, int width, int bottom, float opacity) {
        InventorySnapshot inv = snapshot.inventory();
        int row = beginDataCard(x, y, width, bottom, "INVENTORY", "41 fixed model slots  /  selected " + inv.selectedSlot(), UiTheme.WARNING, opacity);
        row = dataRow(x, row, width, bottom, "Resources", "Fe " + inv.iron() + "   Au " + inv.gold() + "   D " + inv.diamonds() + "   E " + inv.emeralds() + "   W " + inv.wool(), UiTheme.WARNING, opacity);
        List<ItemSlotObservation> slots = inv.slots();
        for (int group = 0; group < 9 && row < bottom; group++) {
            StringBuilder line = new StringBuilder();
            int start = group * 5;
            for (int i = start; i < Math.min(start + 5, slots.size()); i++) {
                ItemSlotObservation slot = slots.get(i);
                if (line.length() > 0) line.append("   ");
                line.append(slot.slotIndex()).append(':').append(shortCategory(slot)).append('x').append(slot.count());
            }
            row = dataRow(x, row, width, bottom, group == 0 ? "Hotbar / slots" : "", line.toString(), group < 2 ? UiTheme.TEXT_PRIMARY : UiTheme.TEXT_SECONDARY, opacity);
        }
    }

    private void renderEvents(ObservationSnapshot snapshot, int x, int y, int width, int bottom, float opacity) {
        List<ObservationEvent> events = snapshot.events().events();
        int row = beginDataCard(x, y, width, bottom, "EVENT STREAM",
            events.size() + " retained  /  " + snapshot.events().dropped() + " dropped", UiTheme.PURPLE, opacity);
        int shown = 0;
        for (int index = events.size() - 1; index >= 0 && row < bottom && shown < 8; index--, shown++) {
            ObservationEvent event = events.get(index);
            String value = "t" + event.clientTick() + "  id " + event.trackingId() + "  mag " + one(event.magnitude())
                + "  " + (event.success() ? "OK" : "FAIL");
            row = dataRow(x, row, width, bottom, event.type().name(), value,
                event.success() ? UiTheme.TEXT_PRIMARY : UiTheme.DANGER, opacity);
        }
        if (events.isEmpty()) {
            dataRow(x, row, width, bottom, "Empty", "No recent bounded events", UiTheme.TEXT_SECONDARY, opacity);
        }
    }

    private void renderDifference(ObservationSnapshot snapshot, int x, int y, int width, int bottom, float opacity) {
        ObservationDiff d = inspector.latestDiff();
        int row = beginDataCard(x, y, width, bottom, "SNAPSHOT DIFFERENCE",
            "#" + d.fromSequence() + " -> #" + d.toSequence(), UiTheme.ACCENT, opacity);
        row = dataRow(x, row, width, bottom, "Client tick delta", Long.toString(d.clientTickDelta()), UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Movement / yaw", three(d.positionDistance()) + " / " + one(d.yawDeltaDegrees()), UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Terrain / map", d.terrainChangedCells() + " cells / " + d.mapChangedColumns() + " columns", UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Inventory", d.inventoryChangedSlots() + " slots changed", UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Entities", "+" + d.entitiesAdded() + "  -" + d.entitiesRemoved() + "  ~" + d.entitiesChanged(), UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Validity XOR", "0x" + Long.toHexString(d.validityChangedBits()), UiTheme.TEXT_PRIMARY, opacity);
        dataRow(x, row, width, bottom, "Current", "#" + snapshot.sequenceNumber() + (d.isEmpty() ? "  no encoded changes" : "  changed"), d.isEmpty() ? UiTheme.SAFE : UiTheme.WARNING, opacity);
    }

    private void renderSystem(ObservationSnapshot snapshot, int x, int y, int width, int bottom, float opacity) {
        SensorTimings t = snapshot.sensorTimings();
        int row = beginDataCard(x, y, width, bottom, "SYSTEM HEALTH",
            snapshot.schemaVersion() + "  /  " + snapshot.taskAdapterIdentifier(), UiTheme.SAFE, opacity);
        row = dataRow(x, row, width, bottom, "Self / terrain", micros(t.selfNanos()) + " / " + micros(t.terrainNanos()) + " us", UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Map / entities", micros(t.midRangeNanos()) + " / " + micros(t.entitiesNanos()) + " us", UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Inventory / events", micros(t.inventoryNanos()) + " / " + micros(t.eventsNanos()) + " us", UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "Sensor total", micros(t.totalNanos()) + " us  /  interval " + observations.intervalTicks() + " ticks", freshnessColor(observations.snapshotAgeMillis()), opacity);
        row = dataRow(x, row, width, bottom, "Client handler", micros(tickTiming.averageNanos()) + " avg  /  " + micros(tickTiming.maximumNanos()) + " max us", UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "World overlay", micros(worldRenderer.averageRenderNanos()) + " avg  /  " + micros(worldRenderer.maximumRenderNanos()) + " max us", UiTheme.TEXT_PRIMARY, opacity);
        row = dataRow(x, row, width, bottom, "HUD", micros(hudTiming.averageNanos()) + " avg  /  " + micros(hudTiming.maximumNanos()) + " max us", UiTheme.ACCENT, opacity);
        dataRow(x, row, width, bottom, "Export", exports.status() + "  queue " + exports.queueSize() + "/" + exports.queueCapacity(), exportColor(), opacity);
    }

    private void drawOverlayControls(int x, int y, int width, float opacity) {
        int gap = 4;
        int chipWidth = (width - gap * 4) / 5;
        overlayChip(x, y, chipWidth, "GRID", keys.toggleTerrainOverlay, state.terrainOverlayVisible(), UiTheme.CYAN, opacity);
        overlayChip(x + (chipWidth + gap), y, chipWidth, "COLLISION", keys.toggleCollisionOverlay, state.collisionOverlayVisible(), UiTheme.SAFE, opacity);
        overlayChip(x + (chipWidth + gap) * 2, y, chipWidth, "ENTITIES", keys.toggleEntityOverlay, state.entityOverlayVisible(), UiTheme.PURPLE, opacity);
        overlayChip(x + (chipWidth + gap) * 3, y, chipWidth, "TRACERS", keys.toggleEntityTracers, state.entityTracersVisible(), UiTheme.ACCENT, opacity);
        overlayChip(x + (chipWidth + gap) * 4, y, width - (chipWidth + gap) * 4, "MARKS", keys.toggleLandmarkOverlay, state.landmarkOverlayVisible(), UiTheme.YELLOW, opacity);
    }

    private void overlayChip(int x, int y, int width, String label, net.minecraft.client.settings.KeyBinding binding,
                             boolean active, int color, float opacity) {
        ui.roundedRect(x, y, width, 24, UiTheme.RADIUS_CHIP,
            UiTheme.withOpacity(active ? color : UiTheme.GLASS_TERTIARY, opacity));
        String key = KeyLabel.of(binding);
        ui.text(key, x + 5, y + 4, active ? 0xFF101318 : UiTheme.TEXT_PRIMARY, opacity);
        ui.text(ui.fit(label, width - 10), x + 5, y + 14,
            active ? 0xCC101318 : UiTheme.TEXT_TERTIARY, opacity);
    }

    private void drawFooter(int x, int y, int width, float opacity) {
        ui.divider(x, y - 6, width, opacity);
        String left = KeyLabel.of(keys.toggleFreeze) + " Freeze   " + KeyLabel.of(keys.stepObservation)
            + " Step   " + KeyLabel.of(keys.exportSnapshot) + " Export";
        String right = KeyLabel.of(keys.previousEntity) + "/" + KeyLabel.of(keys.nextEntity) + " Target   "
            + KeyLabel.of(keys.manualTakeover) + " Takeover";
        ui.text(ui.fit(left, width * 3 / 5), x, y, UiTheme.TEXT_SECONDARY, opacity);
        ui.textRight(ui.fit(right, width * 2 / 5), x + width, y, UiTheme.TEXT_TERTIARY, opacity);
    }

    private void drawToast(int screenWidth, int statusY, int statusWidth, long now) {
        boolean visible = !lastNotice.isEmpty() && now - noticeChangedNanos < TOAST_VISIBLE_NANOS;
        float reveal = toastReveal.update(visible, animationsEnabled, now);
        if (reveal <= 0.01F) return;
        float opacity = ease(reveal);
        int width = Math.min(230, Math.max(150, ui.textWidth(lastNotice) + 34));
        int x = Math.max(8, screenWidth - width - 8);
        int y = statusY + Math.round((1F - opacity) * -6F);
        int color = noticeColor(lastNotice);
        ui.panel(x, y, width, 31, UiTheme.RADIUS_CARD, UiTheme.GLASS, opacity);
        ui.dot(x + 12, y + 15, 3, color, opacity);
        ui.text(ui.fit(lastNotice, width - 30), x + 22, y + 11, UiTheme.TEXT_PRIMARY, opacity);
    }

    private void updateNotice(long now) {
        String notice = state.inspectorNotice();
        if (!notice.equals(lastNotice)) {
            lastNotice = notice;
            noticeChangedNanos = now;
            toastReveal.snap(false, now);
        }
        String exportStatus = exports.status();
        if (!"idle".equals(exportStatus) && exportStatus.startsWith("saved") && !exportStatus.equals(lastNotice)) {
            lastNotice = exportStatus;
            noticeChangedNanos = now;
            toastReveal.snap(false, now);
        }
    }

    private void drawPageDots(int x, int y, int selected, int count, float opacity) {
        for (int index = 0; index < count; index++) {
            ui.dot(x + index * 8, y, index == selected ? 2 : 1,
                index == selected ? UiTheme.ACCENT : UiTheme.TEXT_TERTIARY, opacity);
        }
    }

    private int beginDataCard(int x, int y, int width, int bottom, String title, String subtitle,
                              int accent, float opacity) {
        int height = Math.max(36, bottom - y);
        ui.card(x, y, width, height, opacity);
        ui.roundedRect(x, y + 8, 3, 20, 2, UiTheme.withOpacity(accent, opacity));
        ui.text(title, x + 9, y + 7, UiTheme.TEXT_PRIMARY, opacity);
        ui.text(ui.fit(subtitle, width - 18), x + 9, y + 20, UiTheme.TEXT_TERTIARY, opacity);
        ui.divider(x + 8, y + 34, width - 16, opacity);
        return y + 40;
    }

    private int dataRow(int x, int y, int width, int bottom, String label, String value, int valueColor, float opacity) {
        if (y + 13 >= bottom) return bottom;
        int labelWidth = Math.min(104, Math.max(68, width / 3));
        if (!label.isEmpty()) ui.text(ui.fit(label, labelWidth - 8), x + 8, y + 2, UiTheme.TEXT_TERTIARY, opacity);
        ui.text(ui.fit(value, width - labelWidth - 16), x + labelWidth, y + 2, valueColor, opacity);
        ui.divider(x + 8, y + 13, width - 16, opacity * 0.45F);
        return y + 15;
    }

    private void drawMetricCard(int x, int y, int width, int height, String label, String value,
                                String detail, int accent, float opacity) {
        ui.card(x, y, width, height, opacity);
        ui.text(label, x + 7, y + 6, UiTheme.TEXT_TERTIARY, opacity);
        ui.text(ui.fit(value, width - 14), x + 7, y + 18, accent, opacity);
        ui.text(ui.fit(detail, width - 14), x + 7, y + 30, UiTheme.TEXT_SECONDARY, opacity);
    }

    private void drawEmptyState(int x, int y, int width, int height, String title, String detail,
                                int accent, float opacity) {
        ui.card(x, y, width, Math.max(48, height), opacity);
        int centerY = y + Math.max(24, height / 2);
        ui.dot(x + 18, centerY, 5, accent, opacity);
        ui.text(title, x + 32, centerY - 9, UiTheme.TEXT_PRIMARY, opacity);
        ui.text(ui.fit(detail, width - 42), x + 32, centerY + 4, UiTheme.TEXT_SECONDARY, opacity);
    }

    private int exportColor() {
        String status = exports.status();
        return status.contains("failure") || status.contains("rejected") ? UiTheme.DANGER
            : "idle".equals(status) ? UiTheme.TEXT_SECONDARY : UiTheme.SAFE;
    }

    private static int freshnessColor(long ageMillis) {
        if (ageMillis <= 250L) return UiTheme.SAFE;
        if (ageMillis <= 600L) return UiTheme.WARNING;
        return UiTheme.DANGER;
    }

    private static int noticeColor(String notice) {
        String lower = notice.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("failure") || lower.contains("rejected") || lower.contains("emergency")) return UiTheme.DANGER;
        if (lower.contains("frozen") || lower.contains("queued")) return UiTheme.CYAN;
        if (lower.contains("off") || lower.contains("disabled")) return UiTheme.WARNING;
        return UiTheme.SAFE;
    }

    private static String pretty(String value) {
        if (value == null || value.isEmpty()) return "Inspector";
        String lower = value.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String shortCategory(ItemSlotObservation slot) {
        String value = slot.category().name();
        return value.length() > 6 ? value.substring(0, 6) : value;
    }

    private static String flag(boolean active, String label) { return active ? label : "-"; }
    private static float ease(float value) { float bounded = Math.max(0F, Math.min(1F, value)); return 1F - (1F - bounded) * (1F - bounded); }
    private static long micros(long nanos) { return nanos / 1_000L; }

    private static String one(float value) {
        if (Float.isNaN(value)) return "NaN";
        if (Float.isInfinite(value)) return value > 0F ? "Inf" : "-Inf";
        boolean negative = value < 0F;
        long scaled = Math.round(Math.abs(value) * 10F);
        return (negative ? "-" : "") + (scaled / 10L) + "." + (scaled % 10L);
    }

    private static String three(double value) {
        boolean negative = value < 0D;
        long scaled = Math.round(Math.abs(value) * 1000D);
        return (negative ? "-" : "") + (scaled / 1000L) + "." + pad3(scaled % 1000L);
    }

    private static String pad3(long value) {
        if (value < 10L) return "00" + value;
        if (value < 100L) return "0" + value;
        return Long.toString(value);
    }

    public long averageRenderNanos() { return hudTiming.averageNanos(); }
    public long maximumRenderNanos() { return hudTiming.maximumNanos(); }
}
