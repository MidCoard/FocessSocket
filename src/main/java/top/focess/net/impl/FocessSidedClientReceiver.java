package top.focess.net.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.jetbrains.annotations.NotNull;
import top.focess.net.PackHandler;
import top.focess.net.packet.*;
import top.focess.scheduler.FocessScheduler;
import top.focess.scheduler.Scheduler;

import java.time.Duration;
import java.util.Queue;

public class FocessSidedClientReceiver extends AClientReceiver {

    private final FocessSidedClientSocket focessSidedClientSocket;
    private final Scheduler scheduler = new FocessScheduler("FocessSidedClientReceiver");

    private final Queue<Packet> packets = Queues.newConcurrentLinkedQueue();

    public FocessSidedClientReceiver(@NotNull final FocessSidedClientSocket focessSidedClientSocket, final String name) {
        super(focessSidedClientSocket.getHost(), focessSidedClientSocket.getPort(), name);
        this.focessSidedClientSocket = focessSidedClientSocket;
        this.scheduler.runTimer(() -> {
            if (this.connected)
                this.packets.offer(new HeartPacket(this.id, this.token, System.currentTimeMillis()));
            else
                focessSidedClientSocket.sendPacket(new SidedConnectPacket(name));
        }, Duration.ZERO, Duration.ofSeconds(2));
        this.scheduler.runTimer(() -> {
            if (this.connected) {
                Packet packet = this.packets.poll();
                if (packet == null)
                    packet = new WaitPacket(this.id, this.token);
                focessSidedClientSocket.sendPacket(packet);
            }
        }, Duration.ZERO, Duration.ofMillis(100));
    }

    @Override
    public void sendPacket(final Packet packet) {
        this.packets.add(new ClientPackPacket(this.id, this.token, packet));
    }

    @PacketHandler
    public void onConnected(final ConnectedPacket packet) {
        if (this.connected)
            return;
        this.token = packet.getToken();
        this.id = packet.getClientId();
        this.connected = true;
    }

    @PacketHandler
    public void onDisconnected(final DisconnectedPacket packet) {
        this.connected = false;
        this.focessSidedClientSocket.sendPacket(new SidedConnectPacket(this.name));
    }

    @PacketHandler
    public void onServerPacket(final ServerPackPacket packet) {
        for (final PackHandler packHandler : this.packHandlers.getOrDefault(packet.getPacket().getClass(), Lists.newArrayList()))
            packHandler.handle(packet.getPacket());
    }

    @Override
    public void close() {
        this.scheduler.close();
        this.unregisterAll();
    }
}
