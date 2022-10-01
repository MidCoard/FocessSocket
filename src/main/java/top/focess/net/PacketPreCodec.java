package top.focess.net;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.focess.net.packet.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * This class is used to prehandle the packet.
 */
public class PacketPreCodec {

    private static final Map<Integer, PacketCodec<? extends Packet>> PACKET_CODECS = Maps.newHashMap();

    static {
        register(MessagePacket.PACKET_ID, new MessagePacketCodec());
        register(HeartPacket.PACKET_ID, new HeartPacketCodec());
        register(ConnectPacket.PACKET_ID, new ConnectPacketCodec());
        register(ConnectedPacket.PACKET_ID, new ConnectedPacketCodec());
        register(DisconnectPacket.PACKET_ID, new DisconnectPacketCodec());
        register(DisconnectedPacket.PACKET_ID, new DisconnectedPacketCodec());
        register(ClientPackPacket.PACKET_ID, new ClientPackPacketCodec());
        register(ServerPackPacket.PACKET_ID, new ServerPackPacketCodec());
        register(SidedConnectPacket.PACKET_ID, new SidedConnectPacketCodec());
        register(WaitPacket.PACKET_ID, new WaitPacketCodec());
    }

    private final List<Byte> data = Lists.newArrayList();

    private int pointer;

    /**
     * Register the packet codec for the special packet id
     *
     * @param packetId    the packet id
     * @param packetCodec the packet codec
     * @param <T>         the packet type
     */
    public static <T extends Packet> void register(final int packetId, final PacketCodec<T> packetCodec) {
        PACKET_CODECS.put(packetId, packetCodec);
    }

    /**
     * Read a integer
     *
     * @return the integer read from precodec
     */
    public int readInt() {
        int r = 0;
        for (int i = 0; i < 4; i++)
            r += Byte.toUnsignedInt(this.data.get(this.pointer++)) << (i * 8);
        return r;
    }

    /**
     * Read a long
     *
     * @return the long read from precodec
     */
    public long readLong() {
        long r = 0L;
        for (int i = 0; i < 8; i++)
            r += Byte.toUnsignedLong(this.data.get(this.pointer++)) << (i * 8L);
        return r;
    }

    /**
     * Write a integer
     *
     * @param v the integer
     */
    public void writeInt(int v) {
        for (int i = 0; i < 4; i++) {
            this.data.add((byte) (v & 0xFF));
            v >>>= 8;
        }
    }

    /**
     * Write a long
     *
     * @param v the long
     */
    public void writeLong(long v) {
        for (int i = 0; i < 8; i++) {
            this.data.add((byte) (v & 0xFFL));
            v >>>= 8;
        }
    }

    /**
     * Write a string
     *
     * @param v the string
     */
    public void writeString(@NotNull final String v) {
        final byte[] bytes = v.getBytes(StandardCharsets.UTF_8);
        this.writeInt(bytes.length);
        this.data.addAll(Bytes.asList(bytes));
    }

    /**
     * Read a string
     *
     * @return the string read from precodec
     */
    public String readString() {
        final int length = this.readInt();
        final byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++)
            bytes[i] = this.data.get(this.pointer++);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Write a float
     *
     * @param v the float
     */
    public void writeFloat(final float v) {
        this.writeInt(Float.floatToIntBits(v));
    }

    /**
     * Read a float
     *
     * @return the float read from precodec
     */
    public float readFloat() {
        return Float.intBitsToFloat(this.readInt());
    }

    /**
     * Write a double
     *
     * @param v the double
     */
    public void writeDouble(final double v) {
        this.writeLong(Double.doubleToLongBits(v));
    }

    /**
     * Read a double
     *
     * @return the double read from precodec
     */
    public double readDouble() {
        return Double.longBitsToDouble(this.readLong());
    }

    /**
     * Read a byte
     *
     * @return the byte read from precodec
     */
    public byte readByte() {
        return this.data.get(this.pointer++);
    }

    /**
     * Write a byte
     *
     * @param b the byte
     */
    public void writeByte(final byte b) {
        this.data.add(b);
    }

    /**
     * Read a short
     *
     * @return the short read from precodec
     */
    public short readShort() {
        short r = 0;
        for (int i = 0; i < 2; i++)
            // still the right side is short even if not cast to short
            // because two bytes are used to represent a short
            r += (short) ((short) Byte.toUnsignedInt(this.data.get(this.pointer++)) << (i * 8));
        return r;
    }

    /**
     * Write a short
     *
     * @param v the short
     */
    public void writeShort(short v) {
        for (int i = 0; i < 2; i++) {
            this.data.add((byte) (v & 0xFF));
            v >>>= 8;
        }
    }

    /**
     * Get all bytes of the packet
     *
     * @return all bytes of the packet
     */
    public byte[] getBytes() {
        return Bytes.toArray(this.data);
    }

    /**
     * Read a packet
     *
     * @return the packet read from precodec
     */
    @Nullable
    public Packet readPacket() {
        final int packetId;
        try {
            packetId = this.readInt();
        } catch (final Exception e) {
            return null;
        }
        final PacketCodec<? extends Packet> packetCodec = PACKET_CODECS.get(packetId);
        if (packetCodec != null)
            return packetCodec.readPacket(this);
        return null;
    }

    /**
     * Write a packet
     *
     * @param packet the packet
     * @param <T>    the packet type
     * @return true if the packet has been written successfully, false otherwise
     */
    public <T extends Packet> boolean writePacket(@NotNull final T packet) {
        final int packetId = packet.getId();
        final PacketCodec<T> packetCodec = (PacketCodec<T>) PACKET_CODECS.get(packetId);
        if (packetCodec != null) {
            this.writeInt(packetId);
            packetCodec.writePacket(packet, this);
            return true;
        }
        return false;
    }

    /**
     * Push the data to the precodec
     *
     * @param buffer the data
     * @param offset the offset of the data
     * @param length the length of the data
     * @see #push(byte[], int)
     */
    public void push(final byte[] buffer, final int offset, final int length) {
        for (int i = offset; i < length; i++)
            this.data.add(buffer[i]);
    }

    /**
     * Push the data to the precodec
     *
     * @param buffer the data
     * @param length the length of the data
     * @see #push(byte[], int, int)
     */
    public void push(final byte[] buffer, final int length) {
        this.push(buffer, 0, length);
    }
}
