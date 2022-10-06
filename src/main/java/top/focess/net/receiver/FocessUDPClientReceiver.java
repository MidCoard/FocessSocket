package top.focess.net.receiver;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import top.focess.net.PackHandler;
import top.focess.net.PacketHandler;
import top.focess.net.packet.*;
import top.focess.net.socket.ASocket;
import top.focess.net.socket.FocessUDPSocket;
import top.focess.scheduler.FocessScheduler;
import top.focess.scheduler.Scheduler;

import java.time.Duration;
import java.util.Queue;

public class FocessUDPClientReceiver extends AClientReceiver{


    private final Scheduler scheduler = new FocessScheduler("FocessUDPClientReceiver");

    private final Queue<ClientPacket> packets = Queues.newConcurrentLinkedQueue();

    public FocessUDPClientReceiver(FocessUDPSocket socket,String localhost, String host, int port, String name, boolean serverHeart, boolean encrypt) {
        super(host, port, name, serverHeart, encrypt);
        this.scheduler.runTimer(() -> {
            if (this.connected)
                this.packets.offer(new HeartPacket(this.id, this.token, System.currentTimeMillis()));
            else
                socket.sendClientPacket(this.getHost(), this.getPort(), new ConnectPacket(localhost, socket.getLocalPort(), name, serverHeart, encrypt, keypair.getPublicKey()));
        }, Duration.ZERO, Duration.ofSeconds(2));
        this.scheduler.runTimer(() -> {
            if (this.connected) {
                ClientPacket packet = this.packets.poll();
                if (packet == null)
                    packet = new WaitPacket(this.id, this.token);
                socket.sendClientPacket(this.getHost(), this.getPort(), packet);
            }
        }, Duration.ZERO, Duration.ofMillis(100));
    }

    public FocessUDPClientReceiver(FocessUDPSocket socket,String localhost, String host, int port, String name) {
        this(socket,localhost, host, port, name, false, false);
    }

    @PacketHandler
    public void onConnected(final ConnectedPacket packet) {
        if (this.connected) {
            if (ASocket.isDebug())
                System.out.println("SC FocessSocket: reject client " + this.name + " connect because of already connected");
            return;
        }
        if (ASocket.isDebug())
            System.out.println("SC FocessSocket: accept client " + this.name + " connect");
        this.token = packet.getToken();
        this.id = packet.getClientId();
        this.connected = true;
        this.key = packet.getKey();
    }

    @PacketHandler
    public void onServerPacket(final ServerPackPacket packet) {
        if (!this.connected) {
            if (ASocket.isDebug())
                System.out.println("SC FocessSocket: reject client " + this.name + " receive packet because of not connected");
            return;
        }
        if (ASocket.isDebug())
            System.out.println("SC FocessSocket: accept client " + this.name + " receive packet");
        for (final PackHandler packHandler : this.packHandlers.getOrDefault(packet.getPacket().getClass(), Lists.newArrayList()))
            packHandler.handle(this.id, packet.getPacket());
    }

    @Override
    public void sendPacket(Packet packet) {
        this.packets.add(new ClientPackPacket(this.getClientId(), this.getClientToken(), packet));
    }

    @Override
    public void close() {
        this.scheduler.close();
        this.packets.clear();
        this.unregisterAll();
    }
}
