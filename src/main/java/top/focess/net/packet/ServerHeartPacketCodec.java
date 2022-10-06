package top.focess.net.packet;

import org.checkerframework.checker.nullness.qual.Nullable;
import top.focess.net.PacketPreCodec;

/**
 * Codec for ServerHeartPacket.
 */
public class ServerHeartPacketCodec extends PacketCodec<ServerHeartPacket> {


    @Override
    public @Nullable ServerHeartPacket readPacket(PacketPreCodec packetPreCodec) {
        return new ServerHeartPacket(packetPreCodec.readLong());
    }

    @Override
    public void writePacket(ServerHeartPacket packet, PacketPreCodec packetPreCodec) {
        packetPreCodec.writeLong(packet.getTime());
    }
}
