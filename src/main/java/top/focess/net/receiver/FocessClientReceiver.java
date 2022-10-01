package top.focess.net.receiver;

import com.google.common.collect.Lists;
import top.focess.net.PackHandler;
import top.focess.net.socket.FocessSocket;
import top.focess.net.PacketHandler;
import top.focess.net.packet.*;
import top.focess.scheduler.FocessScheduler;
import top.focess.scheduler.Scheduler;

import java.time.Duration;

public class FocessClientReceiver extends AClientReceiver {

    private final String localhost;
    private final FocessSocket focessSocket;
    private final Scheduler scheduler = new FocessScheduler("FocessClientReceiver");
    private volatile boolean connected;

    public FocessClientReceiver(final FocessSocket focessSocket, final String localhost, final String host, final int port, final String name) {
        super(host, port, name);
        this.localhost = localhost;
        this.focessSocket = focessSocket;
        this.scheduler.runTimer(() -> {
            if (this.connected)
                focessSocket.sendPacket(host, port, new HeartPacket(this.id, this.token, System.currentTimeMillis()));
            else
                focessSocket.sendPacket(this.host, this.port, new ConnectPacket(localhost, focessSocket.getLocalPort(), name));
        }, Duration.ZERO, Duration.ofSeconds(2));
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
        this.focessSocket.sendPacket(this.host, this.port, new ConnectPacket(this.localhost, this.focessSocket.getLocalPort(), this.name));
    }

    @PacketHandler
    public void onServerPacket(final ServerPackPacket packet) {
        for (final PackHandler packHandler : this.packHandlers.getOrDefault(packet.getPacket().getClass(), Lists.newArrayList()))
            packHandler.handle(packet.getPacket());
    }

    @Override
    public void sendPacket(final Packet packet) {
        this.focessSocket.sendPacket(this.host, this.port, new ClientPackPacket(this.id, this.token, packet));
    }

    @Override
    public void close() {
        this.scheduler.close();
        this.unregisterAll();
    }
}
