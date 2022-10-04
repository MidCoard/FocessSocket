package top.focess.net.packet;

import top.focess.net.PacketPreCodec;

/**
 * Codec for ConnectedPacket.
 */
public class ConnectedPacketCodec extends PacketCodec<ConnectedPacket> {
    @Override
    public ConnectedPacket readPacket(final PacketPreCodec packetPreCodec) {
        final int clientId = packetPreCodec.readInt();
        final String token = packetPreCodec.readString();
        final String key = packetPreCodec.tryReadString();
        return new ConnectedPacket(clientId, token, key);
    }

    @Override
    public void writePacket(final ConnectedPacket packet, final PacketPreCodec packetPreCodec) {
        packetPreCodec.writeInt(packet.getClientId());
        packetPreCodec.writeString(packet.getToken());
        packetPreCodec.tryWriteString(packet.getKey());
    }
}
