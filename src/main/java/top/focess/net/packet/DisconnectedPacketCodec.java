package top.focess.net.packet;

import top.focess.net.PacketPreCodec;

/**
 * Codec for DisconnectedPacket.
 */
public class DisconnectedPacketCodec extends PacketCodec<DisconnectedPacket> {
    @Override
    public DisconnectedPacket readPacket(final PacketPreCodec packetPreCodec) {
        return new DisconnectedPacket();
    }

    @Override
    public void writePacket(final DisconnectedPacket packet, final PacketPreCodec packetPreCodec) {
    }
}
