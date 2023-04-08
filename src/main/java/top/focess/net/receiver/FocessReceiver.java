package top.focess.net.receiver;

import org.jetbrains.annotations.NotNull;
import top.focess.net.PacketHandler;
import top.focess.net.SimpleClient;
import top.focess.net.packet.*;
import top.focess.net.socket.ASocket;
import top.focess.net.socket.BothSideSocket;

public class FocessReceiver extends AServerReceiver {

    public FocessReceiver(final BothSideSocket socket) {
        super(socket);
    }

    @PacketHandler
    public synchronized void onConnect(final ConnectPacket packet) {
        if (ASocket.isDebug())
            System.out.println("FS FocessSocket: client " + packet.getName() + " try connecting from " + packet.getHost() + ":" + packet.getPort());
        for (final SimpleClient simpleClient : this.clientInfos.values())
            if (simpleClient.getName().equals(packet.getName())) {
                if (ASocket.isDebug())
                    System.out.println("FS FocessSocket: server reject client " + packet.getName() + " connecting from " + packet.getHost() + ":" + packet.getPort() + " because of name conflict");
                return;
            }
        final SimpleClient simpleClient = new SimpleClient(packet.getHost(), packet.getPort(), this.defaultClientId.incrementAndGet(), packet.getName(), generateToken(), packet.isServerHeart(), packet.isEncrypt(), packet.getKey());
        this.lastHeart.put(simpleClient.getId(), System.currentTimeMillis());
        this.clientInfos.put(simpleClient.getId(), simpleClient);
        if (ASocket.isDebug())
            System.out.println("FS FocessSocket: server accept client " + packet.getName() + " connecting from " + packet.getHost() + ":" + packet.getPort());
        ((BothSideSocket) this.socket).sendServerPacket(simpleClient, new ConnectedPacket(simpleClient.getId(), simpleClient.getToken(), simpleClient.getPublicKey()));
    }

    @PacketHandler
    public synchronized void onDisconnect(@NotNull final DisconnectPacket packet) {
        if (ASocket.isDebug())
            System.out.println("FS FocessSocket: client " + packet.getClientId() + " try disconnecting");
        if (this.clientInfos.get(packet.getClientId()) == null) {
            if (ASocket.isDebug())
                System.out.println("FS FocessSocket: server reject client " + packet.getClientId() + " disconnecting because of client not exist");
            return;
        }
        final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
        if (simpleClient.getToken().equals(packet.getToken())) {
            if (ASocket.isDebug())
                System.out.println("FS FocessSocket: server accept client " + packet.getClientId() + " disconnecting");
            this.disconnect(packet.getClientId());
        } else if (ASocket.isDebug())
            System.out.println("FS FocessSocket: server reject client " + packet.getClientId() + " disconnecting because of token conflict");

    }

    @Override
    public void sendPacket(String client, Packet packet) {
        ((BothSideSocket) this.socket).sendServerPacket(this.getClient(client), new ServerPackPacket(packet));
    }

    @Override
    public void sendPacket(int id, Packet packet) {
        ((BothSideSocket) this.socket).sendServerPacket(this.getClient(id), new ServerPackPacket(packet));
    }

    @Override
    public void sendPacket(int id, ServerPacket packet) {
        ((BothSideSocket) this.socket).sendServerPacket(this.getClient(id), packet);
    }
}
