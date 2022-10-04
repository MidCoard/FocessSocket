package top.focess.net.packet;


import top.focess.net.PacketPreCodec;

/**
 * Codec for SidedConnectPacket.
 */
public class SidedConnectPacketCodec extends PacketCodec<SidedConnectPacket> {

    @Override
    public SidedConnectPacket readPacket(final PacketPreCodec packetPreCodec) {
        String name = packetPreCodec.readString();
        boolean serverHeart = packetPreCodec.readBoolean();
        boolean encrypt = packetPreCodec.readBoolean();
        String key = packetPreCodec.tryReadString();
        return new SidedConnectPacket(name, serverHeart, encrypt, key);
    }

    @Override
    public void writePacket(final SidedConnectPacket packet, final PacketPreCodec packetPreCodec) {
        packetPreCodec.writeString(packet.getName());
        packetPreCodec.writeBoolean(packet.isServerHeart());
        packetPreCodec.writeBoolean(packet.isEncrypt());
        packetPreCodec.tryWriteString(packet.getKey());
    }
}
