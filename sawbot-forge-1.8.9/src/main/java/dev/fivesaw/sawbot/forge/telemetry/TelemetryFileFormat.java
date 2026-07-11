package dev.fivesaw.sawbot.forge.telemetry;

import dev.fivesaw.sawbot.common.observation.ObservationSnapshot;
import dev.fivesaw.sawbot.common.versioning.SchemaVersion;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/** Stable little-endian framing for recoverable structured trajectory files. */
public final class TelemetryFileFormat {
    public static final byte[] FILE_MAGIC = "SBTEL001".getBytes(StandardCharsets.US_ASCII);
    public static final int FILE_VERSION = 1;
    public static final int ENDIAN_MARKER = 0x01020304;
    public static final int RECORD_MAGIC = 0x31524253; // SBR1
    public static final int RECORD_STEP = 1;
    public static final int RECORD_FOOTER = 2;
    public static final int FLAG_DEFLATE = 1;

    private TelemetryFileFormat() { }

    static void writeHeader(OutputStream output, long createdEpochMillis, ObservationSnapshot snapshot)
            throws IOException {
        LittleEndianOutput header = new LittleEndianOutput(256);
        header.writeBytes(FILE_MAGIC);
        header.writeInt(FILE_VERSION);
        header.writeInt(ENDIAN_MARKER);
        header.writeInt(FLAG_DEFLATE);
        header.writeLong(createdEpochMillis);
        UUID episode = snapshot.episodeId();
        header.writeLong(episode.getMostSignificantBits());
        header.writeLong(episode.getLeastSignificantBits());
        header.writeUtf8(SchemaVersion.TELEMETRY_V0_1.identifier(), 48);
        header.writeUtf8(snapshot.schemaVersion().identifier(), 48);
        header.writeUtf8(snapshot.worldIdentifier(), 96);
        header.writeUtf8(snapshot.taskAdapterIdentifier(), 48);
        byte[] bytes = header.toByteArray();
        output.write(bytes);
    }

    static EncodedRecord encodeRecord(int recordType, long ordinal, long observationSequence,
                                      byte[] uncompressed, int compressionLevel) {
        CRC32 crc = new CRC32();
        crc.update(uncompressed, 0, uncompressed.length);
        byte[] compressed = deflate(uncompressed, compressionLevel);
        return new EncodedRecord(recordType, ordinal, observationSequence, uncompressed.length,
            compressed, (int) crc.getValue(), uncompressed);
    }

    static void writeRecord(OutputStream output, EncodedRecord record) throws IOException {
        LittleEndianOutput header = new LittleEndianOutput(40);
        header.writeInt(RECORD_MAGIC);
        header.writeByte(record.recordType);
        header.writeByte(FLAG_DEFLATE);
        header.writeShort(0);
        header.writeLong(record.ordinal);
        header.writeLong(record.observationSequence);
        header.writeInt(record.uncompressedLength);
        header.writeInt(record.compressed.length);
        header.writeInt(record.crc32);
        output.write(header.toByteArray());
        output.write(record.compressed);
    }

    private static byte[] deflate(byte[] input, int level) {
        Deflater deflater = new Deflater(Math.max(0, Math.min(6, level)));
        try {
            deflater.setInput(input);
            deflater.finish();
            byte[] chunk = new byte[Math.max(1024, input.length / 2)];
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream(input.length / 2);
            while (!deflater.finished()) {
                int count = deflater.deflate(chunk);
                if (count <= 0 && deflater.needsInput()) break;
                output.write(chunk, 0, count);
            }
            return output.toByteArray();
        } finally {
            deflater.end();
        }
    }

    static final class EncodedRecord {
        final int recordType;
        final long ordinal;
        final long observationSequence;
        final int uncompressedLength;
        final byte[] compressed;
        final int crc32;
        final byte[] uncompressed;

        EncodedRecord(int recordType, long ordinal, long observationSequence, int uncompressedLength,
                      byte[] compressed, int crc32, byte[] uncompressed) {
            this.recordType = recordType;
            this.ordinal = ordinal;
            this.observationSequence = observationSequence;
            this.uncompressedLength = uncompressedLength;
            this.compressed = compressed;
            this.crc32 = crc32;
            this.uncompressed = uncompressed;
        }
    }
}
