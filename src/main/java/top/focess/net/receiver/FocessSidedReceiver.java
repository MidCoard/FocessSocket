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
import top.focess.net.socket.Socket;
import top.focess.scheduler.FocessScheduler;
import top.focess.scheduler.Scheduler;

import java.time.Duration;
import java.util.Map;
import java.util.Queue;

public class FocessSidedReceiver extends DefaultServerReceiver {

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
        final SimpleClient simpleClient = new SimpleClient(this.defaultClientId++, packet.getName(), generateToken());
        this.lastHeart.put(simpleClient.getId(), System.currentTimeMillis());
        this.clientInfos.put(simpleClient.getId(), simpleClient);
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: server accept client " + packet.getName() + " connect");
        return new ConnectedPacket(simpleClient.getId(), simpleClient.getToken());
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
}
