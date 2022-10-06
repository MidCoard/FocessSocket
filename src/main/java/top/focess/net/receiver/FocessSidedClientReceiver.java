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
    private final Queue<ClientPacket> packets = Queues.newConcurrentLinkedQueue();

    public FocessSidedClientReceiver(@NotNull final FocessSidedClientSocket socket, final String name, final boolean serverHeart, final boolean encrypt) {
        super(new FocessScheduler("FocessSidedClientReceiver"), socket.getHost(), socket.getPort(), name, serverHeart, encrypt);
        this.scheduler.runTimer(() -> {
            if (this.connected)
                this.packets.offer(new HeartPacket(this.id, this.token, System.currentTimeMillis()));
            else
                socket.sendPacket(new SidedConnectPacket(name, serverHeart, encrypt, keypair.getPublicKey()));
        }, Duration.ZERO, Duration.ofSeconds(2));
        this.scheduler.runTimer(() -> {
            if (this.connected) {
                Packet packet = this.packets.poll();
                if (packet == null)
                    packet = new WaitPacket(this.id, this.token);
                socket.sendPacket(packet);
            }
        }, Duration.ZERO, Duration.ofMillis(100));
    }

    @Override
    public void sendPacket(final Packet packet) {
        this.packets.add(new ClientPackPacket(this.id, this.token, packet));
    }

    @Override
    public void close() {
        this.scheduler.close();
        this.packets.clear();
        this.unregisterAll();
    }
}
