package top.focess.net.packet;


import top.focess.net.PacketPreCodec;

/**
 * Codec for SidedConnectPacket.
 */
public class SidedConnectPacketCodec extends PacketCodec<SidedConnectPacket> {

    @Override
    public SidedConnectPacket readPacket(final PacketPreCodec packetPreCodec) {
        return new SidedConnectPacket(packetPreCodec.readString());
    }

    @Override
    public void writePacket(final SidedConnectPacket packet, final PacketPreCodec packetPreCodec) {
        packetPreCodec.writeString(packet.getName());
    }
}
