package dev.fivesaw.sawbot.forge.model;

import dev.fivesaw.sawbot.common.action.AbortCondition;
import dev.fivesaw.sawbot.common.action.ActionCommand;
import dev.fivesaw.sawbot.common.action.Skill;
import dev.fivesaw.sawbot.common.action.TacticalObjective;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/** Versioned bounded loopback protocol between the visible client and a local model process. */
public final class ModelProtocol {
    public static final String IDENTIFIER = "sawbot.bridge/0.1";
    public static final int MAGIC = 0x53424D31; // SBM1
    public static final int VERSION = 1;
    public static final int MAX_PAYLOAD_BYTES = 262144;

    public static final int TYPE_HELLO = 1;
    public static final int TYPE_HELLO_ACK = 2;
    public static final int TYPE_OBSERVATION = 3;
    public static final int TYPE_ACTION = 4;
    public static final int TYPE_PING = 5;
    public static final int TYPE_PONG = 6;
    public static final int TYPE_ERROR = 7;
    public static final int TYPE_GOODBYE = 8;

    private ModelProtocol() { }

    public static void writeFrame(DataOutputStream output, int type, byte[] payload) throws IOException {
        if (output == null || payload == null) throw new IllegalArgumentException("frame");
        if (type < 1 || type > 255) throw new IllegalArgumentException("type");
        if (payload.length > MAX_PAYLOAD_BYTES) throw new IllegalArgumentException("payload too large");
        CRC32 crc = new CRC32();
        crc.update(payload, 0, payload.length);
        output.writeInt(MAGIC);
        output.writeByte(VERSION);
        output.writeByte(type);
        output.writeShort(0);
        output.writeInt(payload.length);
        output.writeInt((int) crc.getValue());
        output.write(payload);
        output.flush();
    }

    public static Frame readFrame(DataInputStream input) throws IOException {
        if (input == null) throw new IllegalArgumentException("input");
        int magic;
        try {
            magic = input.readInt();
        } catch (EOFException eof) {
            throw eof;
        }
        if (magic != MAGIC) throw new IOException("bridge frame magic mismatch");
        int version = input.readUnsignedByte();
        if (version != VERSION) throw new IOException("unsupported bridge frame version " + version);
        int type = input.readUnsignedByte();
        input.readUnsignedShort();
        int length = input.readInt();
        int expectedCrc = input.readInt();
        if (length < 0 || length > MAX_PAYLOAD_BYTES) throw new IOException("invalid bridge payload length " + length);
        byte[] payload = new byte[length];
        input.readFully(payload);
        CRC32 crc = new CRC32();
        crc.update(payload, 0, payload.length);
        if ((int) crc.getValue() != expectedCrc) throw new IOException("bridge payload CRC mismatch");
        return new Frame(type, payload);
    }

    public static byte[] encodeHello(String modVersion, String observationSchema, String actionSchema,
                                     long sessionNonce, int decisionRateHz) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(160);
        DataOutputStream out = new DataOutputStream(bytes);
        writeUtf8(out, IDENTIFIER, 48);
        writeUtf8(out, modVersion, 64);
        writeUtf8(out, observationSchema, 48);
        writeUtf8(out, actionSchema, 48);
        out.writeLong(sessionNonce);
        out.writeShort(decisionRateHz);
        out.flush();
        return bytes.toByteArray();
    }

    public static HelloAck decodeHelloAck(byte[] payload, long expectedNonce) throws IOException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
        String protocol = readUtf8(input, 48);
        if (!IDENTIFIER.equals(protocol)) throw new IOException("bridge identifier mismatch");
        String modelVersion = readUtf8(input, 64);
        long nonce = input.readLong();
        int capabilities = input.readInt();
        if (nonce != expectedNonce) throw new IOException("bridge nonce mismatch");
        if (modelVersion.isEmpty()) throw new IOException("empty model version");
        if (input.available() != 0) throw new IOException("unexpected hello-ack bytes");
        return new HelloAck(modelVersion, capabilities);
    }

    public static ActionCommand decodeAction(byte[] payload, String modelVersion, long localTimestampNanos)
            throws IOException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload));
        long observationSequence = input.readLong();
        float forward = input.readFloat();
        float strafe = input.readFloat();
        float yaw = input.readFloat();
        float pitch = input.readFloat();
        float jump = input.readFloat();
        float sprint = input.readFloat();
        float sneak = input.readFloat();
        float attack = input.readFloat();
        float use = input.readFloat();
        float drop = input.readFloat();
        float inventory = input.readFloat();
        int hotbar = input.readByte();
        int skillOrdinal = input.readUnsignedByte();
        int target = input.readInt();
        int waypoint = input.readInt();
        float confidence = input.readFloat();
        int duration = input.readUnsignedByte();
        int objectiveOrdinal = input.readUnsignedByte();
        int abortOrdinal = input.readUnsignedByte();
        if (input.available() != 0) throw new IOException("unexpected action payload bytes");
        Skill[] skills = Skill.values();
        TacticalObjective[] objectives = TacticalObjective.values();
        AbortCondition[] aborts = AbortCondition.values();
        if (skillOrdinal >= skills.length || objectiveOrdinal >= objectives.length || abortOrdinal >= aborts.length) {
            throw new IOException("action enum ordinal out of range");
        }
        return new ActionCommand(observationSequence, localTimestampNanos, modelVersion,
            forward, strafe, yaw, pitch, jump, sprint, sneak, attack, use, drop, inventory,
            hotbar, skills[skillOrdinal], target, waypoint, confidence, duration,
            objectives[objectiveOrdinal], aborts[abortOrdinal]);
    }

    public static byte[] encodePong(byte[] pingPayload) {
        return pingPayload == null ? new byte[0] : pingPayload.clone();
    }

    private static void writeUtf8(DataOutputStream out, String value, int maximumBytes) throws IOException {
        if (value == null) throw new IllegalArgumentException("string");
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maximumBytes || bytes.length > 65535) throw new IllegalArgumentException("string too long");
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private static String readUtf8(DataInputStream input, int maximumBytes) throws IOException {
        int length = input.readUnsignedShort();
        if (length > maximumBytes) throw new IOException("string exceeds bridge bound");
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static final class Frame {
        private final int type;
        private final byte[] payload;
        Frame(int type, byte[] payload) { this.type = type; this.payload = payload; }
        public int type() { return type; }
        public byte[] payload() { return payload.clone(); }
    }

    public static final class HelloAck {
        private final String modelVersion;
        private final int capabilities;
        HelloAck(String modelVersion, int capabilities) {
            this.modelVersion = modelVersion;
            this.capabilities = capabilities;
        }
        public String modelVersion() { return modelVersion; }
        public int capabilities() { return capabilities; }
    }
}
