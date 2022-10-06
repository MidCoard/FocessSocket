package top.focess.net.receiver;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.focess.net.PacketHandler;
import top.focess.net.SimpleClient;
import top.focess.net.packet.*;
import top.focess.net.socket.ASocket;
import top.focess.net.socket.Socket;

import java.util.Map;
import java.util.Queue;

public class FocessSidedReceiver extends AServerReceiver {

    private final Map<String, Queue<Packet>> packets = Maps.newConcurrentMap();

    public FocessSidedReceiver(Socket socket) {
        super(socket);
    }

    @Nullable
    @PacketHandler
    public ConnectedPacket onConnect(final SidedConnectPacket packet) {
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: client " + packet.getName() + " connect");
        for (final SimpleClient simpleClient : this.clientInfos.values())
            if (simpleClient.getName().equals(packet.getName())) {
                if (ASocket.isDebug())
                    System.out.println("P FocessSocket: server reject client " + packet.getName() + " connect because of name conflict");
                return null;
            }
        final SimpleClient simpleClient = new SimpleClient(this.defaultClientId.incrementAndGet(), packet.getName(), generateToken(), packet.isServerHeart(), packet.isEncrypt(), packet.getKey());
        packet.setClientId(simpleClient.getId());
        this.lastHeart.put(simpleClient.getId(), System.currentTimeMillis());
        this.clientInfos.put(simpleClient.getId(), simpleClient);
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: server accept client " + packet.getName() + " connect");
        return new ConnectedPacket(simpleClient.getId(), simpleClient.getToken(), simpleClient.getPublicKey());
    }

    @Nullable
    @PacketHandler
    public Packet onWait(@NotNull final WaitPacket packet) {
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: client " + packet.getClientId() + " send wait");
        if (this.clientInfos.get(packet.getClientId()) != null) {
            final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
            if (simpleClient.getToken().equals(packet.getToken())) {
                if (ASocket.isDebug())
                    System.out.println("P FocessSocket: server accept client " + packet.getClientId() + " wait");
                return this.packets.getOrDefault(simpleClient.getName(), Queues.newConcurrentLinkedQueue()).poll();
            } else if (ASocket.isDebug())
                System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " wait because of token error");
        } else if (ASocket.isDebug())
            System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " send wait because of client not exist");
        return null;
    }

    @PacketHandler
    public void onDisconnect(@NotNull final DisconnectPacket packet) {
        if (ASocket.isDebug())
            System.out.println("FocessSocket " + this + ": client " + packet.getClientId() + " disconnect");
        if (this.clientInfos.get(packet.getClientId()) != null) {
            final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
            if (simpleClient.getToken().equals(packet.getToken())) {
                if (ASocket.isDebug())
                    System.out.println("FocessSocket " + this + ": server accept client " + packet.getClientId() + " disconnect");
                this.disconnect(packet.getClientId());
            } else if (ASocket.isDebug())
                System.out.println("FocessSocket " + this + ": server reject client " + packet.getClientId() + " disconnect because of token conflict");
        } else if (ASocket.isDebug())
            System.out.println("FocessSocket " + this + ": server reject client " + packet.getClientId() + " disconnect because of client not exist");
    }

    public void sendPacket(final String client, final Packet packet) {
        this.packets.compute(client, (k, v) -> {
            if (v == null)
                v = Queues.newConcurrentLinkedQueue();
            v.offer(new ServerPackPacket(packet));
            return v;
        });
    }
}
