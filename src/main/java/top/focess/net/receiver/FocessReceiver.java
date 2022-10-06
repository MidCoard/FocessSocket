package top.focess.net.receiver;

import org.jetbrains.annotations.NotNull;
import top.focess.net.PacketHandler;
import top.focess.net.SimpleClient;
import top.focess.net.packet.ConnectPacket;
import top.focess.net.packet.ConnectedPacket;
import top.focess.net.packet.DisconnectPacket;
import top.focess.net.socket.ASocket;
import top.focess.net.socket.BothSideSocket;

public class FocessReceiver extends AServerReceiver {

    public FocessReceiver(final BothSideSocket socket) {
        super(socket);
    }

    @PacketHandler
    public void onConnect(final ConnectPacket packet) {
        if (ASocket.isDebug())
            System.out.println("S FocessSocket: client " + packet.getName() + " try connecting from " + packet.getHost() + ":" + packet.getPort());
        for (final SimpleClient simpleClient : this.clientInfos.values())
            if (simpleClient.getName().equals(packet.getName())) {
                if (ASocket.isDebug())
                    System.out.println("S FocessSocket: server reject client " + packet.getName() + " connect from " + packet.getHost() + ":" + packet.getPort() + " because of name conflict");
                return;
            }
        final SimpleClient simpleClient = new SimpleClient(packet.getHost(), packet.getPort(), this.defaultClientId.incrementAndGet(), packet.getName(), generateToken(), packet.isServerHeart(), packet.isEncrypt(), packet.getKey());
        this.lastHeart.put(simpleClient.getId(), System.currentTimeMillis());
        this.clientInfos.put(simpleClient.getId(), simpleClient);
        if (ASocket.isDebug())
            System.out.println("S FocessSocket: server accept client " + packet.getName() + " connect from " + packet.getHost() + ":" + packet.getPort());
        ((BothSideSocket) this.socket).sendServerPacket(simpleClient, new ConnectedPacket(simpleClient.getId(), simpleClient.getToken(), simpleClient.getPublicKey()));
    }

    @PacketHandler
    public void onDisconnect(@NotNull final DisconnectPacket packet) {
        if (ASocket.isDebug())
            System.out.println("S FocessSocket: client " + packet.getClientId() + " disconnect");
        if (this.clientInfos.get(packet.getClientId()) != null) {
            final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
            if (simpleClient.getToken().equals(packet.getToken())) {
                if (ASocket.isDebug())
                    System.out.println("S FocessSocket: server accept client " + packet.getClientId() + " disconnect");
                this.disconnect(packet.getClientId());
            } else if (ASocket.isDebug())
                System.out.println("S FocessSocket: server reject client " + packet.getClientId() + " disconnect because of token conflict");
        } else if (ASocket.isDebug())
            System.out.println("S FocessSocket: server reject client " + packet.getClientId() + " disconnect because of client not exist");
    }

}
