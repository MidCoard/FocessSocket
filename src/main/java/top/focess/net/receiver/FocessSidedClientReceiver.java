package top.focess.net.receiver;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import org.jetbrains.annotations.NotNull;
import top.focess.net.PackHandler;
import top.focess.net.PacketHandler;
import top.focess.net.packet.*;
import top.focess.net.socket.ASocket;
import top.focess.net.socket.FocessSidedClientSocket;
import top.focess.scheduler.FocessScheduler;
import top.focess.scheduler.Scheduler;

import java.time.Duration;
import java.util.Queue;

public class FocessSidedClientReceiver extends AClientReceiver {

    private final FocessSidedClientSocket focessSidedClientSocket;
    private final Scheduler scheduler = new FocessScheduler("FocessSidedClientReceiver");
    private final Queue<Packet> packets = Queues.newConcurrentLinkedQueue();

    public FocessSidedClientReceiver(@NotNull final FocessSidedClientSocket focessSidedClientSocket, final String name, final boolean serverHeart, final boolean encrypt) {
        super(focessSidedClientSocket.getHost(), focessSidedClientSocket.getPort(), name, serverHeart, encrypt);
        this.focessSidedClientSocket = focessSidedClientSocket;
        this.scheduler.runTimer(() -> {
            if (this.connected)
                this.packets.offer(new HeartPacket(this.id, this.token, System.currentTimeMillis()));
            else
                focessSidedClientSocket.sendPacket(new SidedConnectPacket(name, serverHeart, encrypt, keypair.getPublicKey()));
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
        if (this.connected) {
            if (ASocket.isDebug())
                System.out.println("PC FocessSocket: reject client " + this.name + " connect because of already connected");
            return;
        }
        if (ASocket.isDebug())
            System.out.println("PC FocessSocket: accept client " + this.name + " connect");
        this.token = packet.getToken();
        this.id = packet.getClientId();
        this.connected = true;
    }

    @PacketHandler
    public void onDisconnected(final DisconnectedPacket packet) {
        if (!this.connected) {
            if (ASocket.isDebug())
                System.out.println("PC FocessSocket: reject client " + this.name + " disconnect because of not connected");
            return;
        }
        if (ASocket.isDebug())
            System.out.println("PC FocessSocket: accept client " + this.name + " disconnect");
        this.connected = false;
        this.focessSidedClientSocket.sendPacket(new SidedConnectPacket(this.name, this.serverHeart, this.encrypt, this.keypair.getPublicKey()));
    }

    @PacketHandler
    public void onServerPacket(final ServerPackPacket packet) {
        if (!this.connected) {
            if (ASocket.isDebug())
                System.out.println("PC FocessSocket: reject client " + this.name + " receive packet because of not connected");
            return;
        }
        if (ASocket.isDebug())
            System.out.println("PC FocessSocket: accept client " + this.name + " receive packet");
        for (final PackHandler packHandler : this.packHandlers.getOrDefault(packet.getPacket().getClass(), Lists.newArrayList()))
            packHandler.handle(this.id, packet.getPacket());
    }

    @Override
    public void close() {
        this.scheduler.close();
        this.packets.clear();
        this.unregisterAll();
    }
}
