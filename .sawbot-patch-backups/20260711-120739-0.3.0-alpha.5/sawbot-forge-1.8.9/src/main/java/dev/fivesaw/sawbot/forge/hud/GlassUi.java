package dev.fivesaw.sawbot.forge.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

/** Allocation-light glass-card renderer for the compact runtime HUD. */
public final class GlassUi {
    private static final int MAX_RADIUS = 12;
    private static final int[][] CORNER_INSETS = buildInsets();
    private final Minecraft minecraft;

    public GlassUi(Minecraft minecraft) {
        if (minecraft == null) throw new IllegalArgumentException("minecraft");
        this.minecraft = minecraft;
    }

    public void panel(int x, int y, int width, int height, int radius, int fill, float opacity) {
        roundedRect(x + 1, y + 3, width, height, radius, UiTheme.withOpacity(UiTheme.SHADOW, opacity));
        roundedRect(x, y, width, height, radius, UiTheme.withOpacity(UiTheme.BORDER, opacity));
        roundedRect(x + 1, y + 1, width - 2, height - 2, Math.max(1, radius - 1), UiTheme.withOpacity(fill, opacity));
    }

    public void card(int x, int y, int width, int height, float opacity) {
        roundedRect(x, y, width, height, UiTheme.RADIUS_CARD, UiTheme.withOpacity(UiTheme.BORDER, opacity));
        roundedRect(x + 1, y + 1, width - 2, height - 2, UiTheme.RADIUS_CARD - 1,
            UiTheme.withOpacity(UiTheme.GLASS_SECONDARY, opacity));
    }

    public void roundedRect(int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0 || ((color >>> 24) & 0xFF) == 0) return;
        int boundedRadius = Math.max(0, Math.min(MAX_RADIUS, Math.min(radius, Math.min(width, height) / 2)));
        if (boundedRadius == 0) {
            Gui.drawRect(x, y, x + width, y + height, color);
            return;
        }
        Gui.drawRect(x + boundedRadius, y, x + width - boundedRadius, y + height, color);
        Gui.drawRect(x, y + boundedRadius, x + width, y + height - boundedRadius, color);
        int[] insets = CORNER_INSETS[boundedRadius];
        for (int row = 0; row < boundedRadius; row++) {
            int inset = insets[row];
            Gui.drawRect(x + inset, y + row, x + width - inset, y + row + 1, color);
            Gui.drawRect(x + inset, y + height - row - 1, x + width - inset, y + height - row, color);
        }
    }

    public void divider(int x, int y, int width, float opacity) {
        Gui.drawRect(x, y, x + width, y + 1, UiTheme.withOpacity(UiTheme.BORDER, opacity));
    }

    public void progress(int x, int y, int width, int height, float progress, int color, float opacity) {
        roundedRect(x, y, width, height, Math.max(1, height / 2), UiTheme.withOpacity(UiTheme.GLASS_TERTIARY, opacity));
        int fillWidth = Math.max(height, Math.round(width * Math.max(0F, Math.min(1F, progress))));
        roundedRect(x, y, Math.min(width, fillWidth), height, Math.max(1, height / 2), UiTheme.withOpacity(color, opacity));
    }

    public void dot(int centerX, int centerY, int radius, int color, float opacity) {
        roundedRect(centerX - radius, centerY - radius, radius * 2, radius * 2, radius,
            UiTheme.withOpacity(color, opacity));
    }

    public void text(String text, int x, int y, int color, float opacity) {
        if (text == null || text.isEmpty()) return;
        FontRenderer font = minecraft.fontRendererObj;
        if (font != null) font.drawStringWithShadow(text, x, y, UiTheme.withOpacity(color, opacity));
    }

    public void textRight(String text, int rightX, int y, int color, float opacity) {
        FontRenderer font = minecraft.fontRendererObj;
        if (font == null || text == null) return;
        text(text, rightX - font.getStringWidth(text), y, color, opacity);
    }

    public void scaledText(String text, int x, int y, int color, float opacity, float scale) {
        if (text == null || text.isEmpty()) return;
        FontRenderer font = minecraft.fontRendererObj;
        if (font == null || scale <= 0F) return;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0D);
        GlStateManager.scale(scale, scale, 1F);
        font.drawStringWithShadow(text, 0F, 0F, UiTheme.withOpacity(color, opacity));
        GlStateManager.popMatrix();
    }

    public int textWidth(String text) {
        FontRenderer font = minecraft.fontRendererObj;
        return font == null || text == null ? 0 : font.getStringWidth(text);
    }

    public String fit(String text, int maxWidth) {
        if (text == null) return "";
        if (textWidth(text) <= maxWidth) return text;
        String suffix = "...";
        int end = text.length();
        while (end > 0 && textWidth(text.substring(0, end) + suffix) > maxWidth) end--;
        return end <= 0 ? suffix : text.substring(0, end) + suffix;
    }

    public void pill(String text, int x, int y, int width, int height, int color, boolean filled, float opacity) {
        int background = filled ? color : UiTheme.GLASS_TERTIARY;
        roundedRect(x, y, width, height, Math.min(UiTheme.RADIUS_CHIP, height / 2), UiTheme.withOpacity(background, opacity));
        if (!filled) {
            roundedRect(x, y, 3, height, Math.min(2, height / 2), UiTheme.withOpacity(color, opacity));
        }
        int textColor = filled ? 0xFF101318 : UiTheme.TEXT_PRIMARY;
        int textX = x + Math.max(4, (width - textWidth(text)) / 2);
        text(text, textX, y + Math.max(2, (height - 8) / 2), textColor, opacity);
    }

    public void robotMark(int x, int y, int size, int accent, float opacity) {
        roundedRect(x, y, size, size, UiTheme.RADIUS_CARD, UiTheme.withOpacity(UiTheme.GLASS_TERTIARY, opacity));
        roundedRect(x + 5, y + 7, size - 10, size - 13, 4, UiTheme.withOpacity(accent, opacity));
        roundedRect(x + 7, y + 9, size - 14, size - 17, 3, UiTheme.withOpacity(0xFF151820, opacity));
        dot(x + 10, y + 14, 1, accent, opacity);
        dot(x + size - 10, y + 14, 1, accent, opacity);
        Gui.drawRect(x + 9, y + size - 7, x + size - 9, y + size - 6, UiTheme.withOpacity(accent, opacity));
    }

    private static int[][] buildInsets() {
        int[][] values = new int[MAX_RADIUS + 1][];
        values[0] = new int[0];
        for (int radius = 1; radius <= MAX_RADIUS; radius++) {
            values[radius] = new int[radius];
            for (int row = 0; row < radius; row++) {
                double y = radius - row - 0.5D;
                values[radius][row] = Math.max(0, (int)Math.ceil(radius - Math.sqrt(radius * radius - y * y)));
            }
        }
        return values;
    }
}
