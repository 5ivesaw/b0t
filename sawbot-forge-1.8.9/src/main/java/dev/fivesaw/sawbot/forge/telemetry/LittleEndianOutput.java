package dev.fivesaw.sawbot.forge.telemetry;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Minimal little-endian binary writer with explicit bounded UTF-8 strings. */
final class LittleEndianOutput {
    private final ByteArrayOutputStream output;

    LittleEndianOutput(int initialCapacity) {
        output = new ByteArrayOutputStream(Math.max(64, initialCapacity));
    }

    void writeByte(int value) { output.write(value & 0xFF); }
    void writeBoolean(boolean value) { writeByte(value ? 1 : 0); }
    void writeShort(int value) {
        writeByte(value);
        writeByte(value >>> 8);
    }
    void writeInt(int value) {
        writeByte(value);
        writeByte(value >>> 8);
        writeByte(value >>> 16);
        writeByte(value >>> 24);
    }
    void writeLong(long value) {
        writeInt((int) value);
        writeInt((int) (value >>> 32));
    }
    void writeFloat(float value) { writeInt(Float.floatToIntBits(value)); }
    void writeDouble(double value) { writeLong(Double.doubleToLongBits(value)); }
    void writeBytes(byte[] bytes) { output.write(bytes, 0, bytes.length); }
    void writeUtf8(String value, int maximumBytes) {
        if (value == null) throw new IllegalArgumentException("value");
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maximumBytes || bytes.length > 65535) throw new IllegalArgumentException("string too long");
        writeShort(bytes.length);
        writeBytes(bytes);
    }
    byte[] toByteArray() { return output.toByteArray(); }
}
