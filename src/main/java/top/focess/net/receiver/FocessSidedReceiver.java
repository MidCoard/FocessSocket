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

    private final Map<String, Queue<ServerPacket>> packets = Maps.newConcurrentMap();

    public FocessSidedReceiver(Socket socket) {
        super(socket);
    }

    @Nullable
    @PacketHandler
    public synchronized ConnectedPacket onConnect(final SidedConnectPacket packet) {
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: client " + packet.getName() + " try connecting");
        for (final SimpleClient simpleClient : this.clientInfos.values())
            if (simpleClient.getName().equals(packet.getName())) {
                if (ASocket.isDebug())
                    System.out.println("P FocessSocket: server reject client " + packet.getName() + " connecting because of name conflict");
                return null;
            }
        final SimpleClient simpleClient = new SimpleClient(this.defaultClientId.incrementAndGet(), packet.getName(), generateToken(), packet.isServerHeart(), packet.isEncrypt(), packet.getKey());
        packet.setClientId(simpleClient.getId());
        this.lastHeart.put(simpleClient.getId(), System.currentTimeMillis());
        this.clientInfos.put(simpleClient.getId(), simpleClient);
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: server accept client " + packet.getName() + " connecting");
        return new ConnectedPacket(simpleClient.getId(), simpleClient.getToken(), simpleClient.getPublicKey());
    }

    @Nullable
    @PacketHandler
    public Packet onWait(@NotNull final WaitPacket packet) {
        synchronized (this) {
            if (this.clientInfos.get(packet.getClientId()) == null) {
                if (ASocket.isDebug())
                    System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " sending wait because of client not exist");
                return null;
            }
        }
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: client " + packet.getClientId() + " sending wait");
        final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
        if (simpleClient.getToken().equals(packet.getToken())) {
            if (ASocket.isDebug())
                System.out.println("P FocessSocket: server accept client " + packet.getClientId() + " sending wait");
            return this.packets.getOrDefault(simpleClient.getName(), Queues.newConcurrentLinkedQueue()).poll();
        } else if (ASocket.isDebug())
            System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " sending wait because of token error");
        return null;
    }

    @PacketHandler
    public synchronized void onDisconnect(@NotNull final DisconnectPacket packet) {
        if (ASocket.isDebug())
            System.out.println("P FocessSocket " + this + ": client " + packet.getClientId() + " try disconnecting");
        if (this.clientInfos.get(packet.getClientId()) == null) {
            if (ASocket.isDebug())
                System.out.println("P FocessSocket " + this + ": server reject client " + packet.getClientId() + " disconnecting because of client not exist");
            return;
        }
        final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
        if (simpleClient.getToken().equals(packet.getToken())) {
            if (ASocket.isDebug())
                System.out.println("P FocessSocket " + this + ": server accept client " + packet.getClientId() + " disconnect");
            this.disconnect(packet.getClientId());
        } else if (ASocket.isDebug())
            System.out.println("P FocessSocket " + this + ": server reject client " + packet.getClientId() + " disconnecting because of token conflict");

    }

    public void sendPacket(final String client, final Packet packet) {
        this.packets.compute(client, (k, v) -> {
            if (v == null)
                v = Queues.newConcurrentLinkedQueue();
            v.offer(new ServerPackPacket(packet));
            return v;
        });
    }

    @Override
    public void sendPacket(int id, Packet packet) {
        this.packets.compute(this.clientInfos.get(id).getName(), (k, v) -> {
            if (v == null)
                v = Queues.newConcurrentLinkedQueue();
            v.offer(new ServerPackPacket(packet));
            return v;
        });
    }

    @Override
    public void sendPacket(int id, ServerPacket packet) {
        this.packets.compute(this.clientInfos.get(id).getName(), (k, v) -> {
            if (v == null)
                v = Queues.newConcurrentLinkedQueue();
            v.offer(packet);
            return v;
        });
    }
}
