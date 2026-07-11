package dev.fivesaw.sawbot.forge.hud;

/** Centralized visual tokens for the SawBot runtime interface. */
public final class UiTheme {
    private UiTheme() { }

    public static final int GLASS = 0xE31A1D25;
    public static final int GLASS_SECONDARY = 0xD9232731;
    public static final int GLASS_TERTIARY = 0xC72D323E;
    public static final int SHADOW = 0x7A000000;
    public static final int BORDER = 0x38FFFFFF;
    public static final int BORDER_STRONG = 0x54FFFFFF;

    public static final int TEXT_PRIMARY = 0xFFF5F7FB;
    public static final int TEXT_SECONDARY = 0xFFB6BECA;
    public static final int TEXT_TERTIARY = 0xFF7F8998;

    public static final int ACCENT = 0xFF68A9FF;
    public static final int SAFE = 0xFF5DE2A5;
    public static final int WARNING = 0xFFFFC857;
    public static final int DANGER = 0xFFFF6B73;
    public static final int CYAN = 0xFF5EE7F7;
    public static final int PURPLE = 0xFFC084FC;
    public static final int YELLOW = 0xFFFFDA6A;

    public static final int SPACE_1 = 2;
    public static final int SPACE_2 = 4;
    public static final int SPACE_3 = 8;
    public static final int SPACE_4 = 12;
    public static final int SPACE_5 = 16;

    public static final int RADIUS_CHIP = 4;
    public static final int RADIUS_CARD = 7;
    public static final int RADIUS_PANEL = 10;

    public static int withOpacity(int argb, float opacity) {
        float bounded = Math.max(0F, Math.min(1F, opacity));
        int alpha = (argb >>> 24) & 0xFF;
        int adjusted = Math.max(0, Math.min(255, Math.round(alpha * bounded)));
        return (adjusted << 24) | (argb & 0x00FFFFFF);
    }

    public static int rgb(int argb) { return argb & 0x00FFFFFF; }
}
