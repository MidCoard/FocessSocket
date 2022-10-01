package top.focess.net.packet;

import top.focess.net.PacketPreCodec;

/**
 * Codec for HeartPacket.
 */
public class HeartPacketCodec extends PacketCodec<HeartPacket> {

    @Override
    public HeartPacket readPacket(final PacketPreCodec packetPreCodec) {
        final int clientId = packetPreCodec.readInt();
        final String token = packetPreCodec.readString();
        final long time = packetPreCodec.readLong();
        return new HeartPacket(clientId, token, time);
    }

    @Override
    public void writePacket(final HeartPacket packet, final PacketPreCodec packetPreCodec) {
        packetPreCodec.writeInt(packet.getClientId());
        packetPreCodec.writeString(packet.getToken());
        packetPreCodec.writeLong(packet.getTime());
    }
}
