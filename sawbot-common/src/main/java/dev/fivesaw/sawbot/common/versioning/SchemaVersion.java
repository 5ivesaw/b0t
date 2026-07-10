package dev.fivesaw.sawbot.common.versioning;

import java.util.Objects;

public final class SchemaVersion {
    public static final SchemaVersion OBSERVATION_V0_1 = new SchemaVersion("sawbot.observation", 0, 1);
    public static final SchemaVersion ACTION_V0_1 = new SchemaVersion("sawbot.action", 0, 1);

    private final String family;
    private final int major;
    private final int minor;

    public SchemaVersion(String family, int major, int minor) {
        if (family == null || family.isEmpty() || family.length() > 24) {
            throw new IllegalArgumentException("family must contain 1..24 characters");
        }
        if (major < 0 || major > 65535 || minor < 0 || minor > 65535) {
            throw new IllegalArgumentException("version components must fit unsigned 16-bit values");
        }
        this.family = family;
        this.major = major;
        this.minor = minor;
    }

    public String family() { return family; }
    public int major() { return major; }
    public int minor() { return minor; }
    public String identifier() { return family + "/" + major + "." + minor; }

    @Override public String toString() { return identifier(); }
    @Override public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof SchemaVersion)) return false;
        SchemaVersion other = (SchemaVersion) value;
        return major == other.major && minor == other.minor && family.equals(other.family);
    }
    @Override public int hashCode() { return Objects.hash(family, major, minor); }
}
