package top.focess.net.receiver;

import top.focess.net.PacketHandler;
import top.focess.net.SimpleClient;
import top.focess.net.packet.ConnectPacket;
import top.focess.net.packet.ConnectedPacket;
import top.focess.net.socket.ASocket;
import top.focess.net.socket.FocessUDPSocket;
import top.focess.net.socket.SendableSocket;

public class FocessUDPReceiver extends DefaultServerReceiver {

    public FocessUDPReceiver(final FocessUDPSocket focessUDPSocket) {
        super(focessUDPSocket);
    }

    @PacketHandler
    public void onConnect(final ConnectPacket packet) {
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: client " + packet.getName() + " connect from " + packet.getHost() + ":" + packet.getPort());
        for (final SimpleClient simpleClient : this.clientInfos.values())
            if (simpleClient.getName().equals(packet.getName())) {
                if (ASocket.isDebug())
                    System.out.println("P FocessSocket: server reject client " + packet.getName() + " connect from " + packet.getHost() + ":" + packet.getPort() + " because of name conflict");
                return;
            }
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: server accept client " + packet.getName() + " connect from " + packet.getHost() + ":" + packet.getPort());
        final SimpleClient simpleClient = new SimpleClient(packet.getHost(), packet.getPort(), this.defaultClientId++, packet.getName(), generateToken());
        this.lastHeart.put(simpleClient.getId(), System.currentTimeMillis());
        this.clientInfos.put(simpleClient.getId(), simpleClient);
        ((SendableSocket) this.socket).sendPacket(packet.getHost(), packet.getPort(), new ConnectedPacket(simpleClient.getId(), simpleClient.getToken()));
    }

}
