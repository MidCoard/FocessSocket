package top.focess.net.socket;

import top.focess.net.SimpleClient;
import top.focess.net.packet.ClientPacket;
import top.focess.net.packet.Packet;
import top.focess.net.packet.ServerPacket;

public interface SendableSocket {

    boolean sendClientPacket(final String host, final int port, final ClientPacket packet);

    boolean sendServerPacket(SimpleClient client, String host, int port, ServerPacket serverPacket);
}
