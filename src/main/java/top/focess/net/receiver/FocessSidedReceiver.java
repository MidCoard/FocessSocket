package top.focess.net.receiver;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.focess.net.PackHandler;
import top.focess.net.PacketHandler;
import top.focess.net.SimpleClient;
import top.focess.net.packet.*;
import top.focess.net.socket.ASocket;
import top.focess.scheduler.FocessScheduler;
import top.focess.scheduler.Scheduler;

import java.time.Duration;
import java.util.Map;
import java.util.Queue;

public class FocessSidedReceiver extends AServerReceiver {

    private final Map<String, Queue<Packet>> packets = Maps.newConcurrentMap();
    private final Scheduler scheduler = new FocessScheduler("FocessSidedReceiver");

    public FocessSidedReceiver() {
        this.scheduler.runTimer(() -> {
            for (final SimpleClient simpleClient : this.clientInfos.values()) {
                final long time = this.lastHeart.getOrDefault(simpleClient.getId(), 0L);
                if (System.currentTimeMillis() - time > 10 * 1000)
                    this.clientInfos.remove(simpleClient.getId());
            }
        }, Duration.ZERO, Duration.ofSeconds(1));
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
        final SimpleClient simpleClient = new SimpleClient(this.defaultClientId++, packet.getName(), generateToken());
        this.lastHeart.put(simpleClient.getId(), System.currentTimeMillis());
        this.clientInfos.put(simpleClient.getId(), simpleClient);
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: server accept client " + packet.getName() + " connect");
        return new ConnectedPacket(simpleClient.getId(), simpleClient.getToken());
    }

    @Nullable
    @PacketHandler
    public DisconnectedPacket onDisconnect(@NotNull final DisconnectPacket packet) {
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: client " + packet.getClientId() + " disconnect");
        if (this.clientInfos.get(packet.getClientId()) != null) {
            final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
            if (simpleClient.getToken().equals(packet.getToken())) {
                if (ASocket.isDebug())
                    System.out.println("P FocessSocket: server accept client " + packet.getClientId() + " disconnect");
                return this.disconnect(packet.getClientId());
            }
            else if (ASocket.isDebug())
                System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " disconnect because of token error");
        } else if (ASocket.isDebug())
            System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " disconnect because of client not exist");
        return null;
    }

    @Nullable
    @PacketHandler
    public Packet onHeart(@NotNull final HeartPacket packet) {
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: client " + packet.getClientId() + " send heart");
        if (this.clientInfos.get(packet.getClientId()) != null) {
            final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
            if (simpleClient.getToken().equals(packet.getToken()) && System.currentTimeMillis() + 5 * 1000 > packet.getTime()) {
                if (ASocket.isDebug())
                    System.out.println("P FocessSocket: server accept client " + packet.getClientId() + " send heart");
                this.lastHeart.put(simpleClient.getId(), packet.getTime());
                return this.packets.getOrDefault(simpleClient.getName(), Queues.newConcurrentLinkedQueue()).poll();
            } else if (ASocket.isDebug())
                System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " heart because of token error");
        } else if (ASocket.isDebug())
            System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " send heart because of client not exist");
        return null;
    }

    @Nullable
    @PacketHandler
    public Packet onClientPacket(@NotNull final ClientPackPacket packet) {
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: client " + packet.getClientId() + " send client packet");
        if (this.clientInfos.get(packet.getClientId()) != null) {
            final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
            if (simpleClient.getToken().equals(packet.getToken())) {
                if (ASocket.isDebug())
                    System.out.println("P FocessSocket: server accept client " + packet.getClientId() + " send client packet");
                for (final PackHandler packHandler : this.packHandlers.getOrDefault(simpleClient.getName(), Maps.newHashMap()).getOrDefault(packet.getPacket().getClass(), Lists.newArrayList()))
                    packHandler.handle(simpleClient.getId(), packet.getPacket());
                return this.packets.getOrDefault(simpleClient.getName(), Queues.newConcurrentLinkedQueue()).poll();
            } else if (ASocket.isDebug())
                System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " client packet because of token error");
        } else if (ASocket.isDebug())
            System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " send client packet because of client not exist");
        return null;
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

    public void sendPacket(final String client, final Packet packet) {
        this.packets.compute(client, (k, v) -> {
            if (v == null)
                v = Queues.newConcurrentLinkedQueue();
            v.offer(new ServerPackPacket(packet));
            return v;
        });
    }

    @NotNull
    @Contract("_ -> new")
    private DisconnectedPacket disconnect(final int clientId) {
        this.clientInfos.remove(clientId);
        return new DisconnectedPacket();
    }

    @Override
    public void close() {
        this.scheduler.close();
        for (final Integer id : this.clientInfos.keySet())
            this.disconnect(id);
        this.unregisterAll();
    }
}
