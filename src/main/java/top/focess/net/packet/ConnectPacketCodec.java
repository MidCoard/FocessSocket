package top.focess.net.packet;

import top.focess.net.PacketPreCodec;

/**
 * Codec for ConnectPacket.
 */
public class ConnectPacketCodec extends PacketCodec<ConnectPacket> {
    @Override
    public ConnectPacket readPacket(final PacketPreCodec packetPreCodec) {
        final String host = packetPreCodec.readString();
        final int port = packetPreCodec.readInt();
        final String name = packetPreCodec.readString();
        final boolean clientHeart = packetPreCodec.readBoolean();
        final boolean encrypt = packetPreCodec.readBoolean();
        final String key = packetPreCodec.tryReadString();
        return new ConnectPacket(host, port, name, encrypt, clientHeart, key);
    }

    @Override
    public void writePacket(final ConnectPacket packet, final PacketPreCodec packetPreCodec) {
        packetPreCodec.writeString(packet.getHost());
        packetPreCodec.writeInt(packet.getPort());
        packetPreCodec.writeString(packet.getName());
        packetPreCodec.writeBoolean(packet.isServerHeart());
        packetPreCodec.writeBoolean(packet.isEncrypt());
        packetPreCodec.tryWriteString(packet.getKey());
    }
}
