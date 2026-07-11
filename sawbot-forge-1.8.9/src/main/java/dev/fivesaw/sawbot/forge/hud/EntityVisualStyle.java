package dev.fivesaw.sawbot.forge.hud;

import dev.fivesaw.sawbot.common.observation.EntityObservation;

/**
 * Single source of truth for the Phase 2 LOS/OCC visual language.
 *
 * <p>Visibility state deliberately has priority over team and selection. A selected
 * entity receives a separate accent outline, so selecting it can never mask a live
 * LOS-to-OCC transition.</p>
 */
public final class EntityVisualStyle {
    public static final int LOS_RGB = 0x55FF55;
    public static final int OCCLUDED_RGB = 0xAA55FF;
    public static final int INCONSISTENT_RGB = 0xFFAA00;
    public static final int SELECTED_ACCENT_RGB = 0xFFFF55;

    private EntityVisualStyle() {
    }

    public static int visibilityRgb(EntityObservation entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity");
        }
        if (entity.lineOfSight() && !entity.occluded()) {
            return LOS_RGB;
        }
        if (!entity.lineOfSight() && entity.occluded()) {
            return OCCLUDED_RGB;
        }
        return INCONSISTENT_RGB;
    }

    public static int visibilityArgb(EntityObservation entity) {
        return 0xFF000000 | visibilityRgb(entity);
    }

    public static int red(int rgb) {
        return (rgb >>> 16) & 0xFF;
    }

    public static int green(int rgb) {
        return (rgb >>> 8) & 0xFF;
    }

    public static int blue(int rgb) {
        return rgb & 0xFF;
    }

    public static String visibilityToken(EntityObservation entity) {
        if (entity.lineOfSight() && !entity.occluded()) {
            return "LOS";
        }
        if (!entity.lineOfSight() && entity.occluded()) {
            return "OCC";
        }
        return "INVALID";
    }
}
