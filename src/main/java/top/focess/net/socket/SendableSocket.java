package top.focess.net.socket;

import top.focess.net.packet.Packet;

public interface SendableSocket {

    boolean sendPacket(final String host, final int port, final Packet packet);
}
