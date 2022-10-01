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
        for (final SimpleClient simpleClient : this.clientInfos.values())
            if (simpleClient.getName().equals(packet.getName()))
                return null;
        final SimpleClient simpleClient = new SimpleClient(this.defaultClientId++, packet.getName(), generateToken());
        this.lastHeart.put(simpleClient.getId(), System.currentTimeMillis());
        this.clientInfos.put(simpleClient.getId(), simpleClient);
        return new ConnectedPacket(simpleClient.getId(), simpleClient.getToken());
    }

    @Nullable
    @PacketHandler
    public DisconnectedPacket onDisconnect(@NotNull final DisconnectPacket packet) {
        if (this.clientInfos.get(packet.getClientId()) != null) {
            final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
            if (simpleClient.getToken().equals(packet.getToken()))
                return this.disconnect(packet.getClientId());
        }
        return null;
    }

    @Nullable
    @PacketHandler
    public Packet onHeart(@NotNull final HeartPacket packet) {
        if (this.clientInfos.get(packet.getClientId()) != null) {
            final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
            if (simpleClient.getToken().equals(packet.getToken()) && System.currentTimeMillis() + 5 * 1000 > packet.getTime()) {
                this.lastHeart.put(simpleClient.getId(), packet.getTime());
                return this.packets.getOrDefault(simpleClient.getName(), Queues.newConcurrentLinkedQueue()).poll();
            }
        }
        return null;
    }

    @Nullable
    @PacketHandler
    public Packet onClientPacket(@NotNull final ClientPackPacket packet) {
        if (this.clientInfos.get(packet.getClientId()) != null) {
            final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
            if (simpleClient.getToken().equals(packet.getToken())) {
                    for (final PackHandler packHandler : this.packHandlers.getOrDefault(simpleClient.getName(), Maps.newHashMap()).getOrDefault(packet.getPacket().getClass(), Lists.newArrayList()))
                        packHandler.handle(packet.getPacket());
                return this.packets.getOrDefault(simpleClient.getName(), Queues.newConcurrentLinkedQueue()).poll();
            }
        }
        return null;
    }

    @Nullable
    @PacketHandler
    public Packet onWait(@NotNull final WaitPacket packet) {
        if (this.clientInfos.get(packet.getClientId()) != null) {
            final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
            if (simpleClient.getToken().equals(packet.getToken()))
                return this.packets.getOrDefault(simpleClient.getName(), Queues.newConcurrentLinkedQueue()).poll();
        }
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
