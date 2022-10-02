package top.focess.net.receiver;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import top.focess.net.PackHandler;
import top.focess.net.PacketHandler;
import top.focess.net.SimpleClient;
import top.focess.net.packet.*;
import top.focess.net.socket.ASocket;
import top.focess.net.socket.FocessUDPSocket;
import top.focess.scheduler.FocessScheduler;
import top.focess.scheduler.Scheduler;

import java.time.Duration;
import java.util.Objects;

public class FocessUDPReceiver extends AServerReceiver {

    private final FocessUDPSocket focessUDPSocket;
    private final Scheduler scheduler = new FocessScheduler("FocessUDPReceiver");

    public FocessUDPReceiver(final FocessUDPSocket focessUDPSocket) {
        this.focessUDPSocket = focessUDPSocket;
        this.scheduler.runTimer(() -> {
            for (final SimpleClient simpleClient : this.clientInfos.values()) {
                final long time = this.lastHeart.getOrDefault(simpleClient.getId(), 0L);
                if (System.currentTimeMillis() - time > 10 * 1000)
                    this.clientInfos.remove(simpleClient.getId());
            }
        }, Duration.ZERO, Duration.ofSeconds(1));
    }

    private void disconnect(final int clientId) {
        final SimpleClient simpleClient = this.clientInfos.remove(clientId);
        if (simpleClient != null)
            this.focessUDPSocket.sendPacket(Objects.requireNonNull(simpleClient.getHost()), simpleClient.getPort(), new DisconnectedPacket());
    }

    @Override
    public void close() {
        this.scheduler.close();
        for (final Integer id : this.clientInfos.keySet())
            this.disconnect(id);
        this.unregisterAll();
    }

    @PacketHandler
    public void onConnect(final ConnectPacket packet) {
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: client " + packet.getName() + " connect from " + packet.getHost() + ":" + packet.getPort());
        for (final SimpleClient simpleClient : this.clientInfos.values())
            if (simpleClient.getName().equals(packet.getName())) {
                if (ASocket.isDebug())
                    System.out.println("P FocessSocket: server reject client " + packet.getName() + " connect from " + packet.getHost() + ":" + packet.getPort() + " because of name conflict");
                return;
            }
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: server accept client " + packet.getName() + " connect from " + packet.getHost() + ":" + packet.getPort());
        final SimpleClient simpleClient = new SimpleClient(packet.getHost(), packet.getPort(), this.defaultClientId++, packet.getName(), generateToken());
        this.lastHeart.put(simpleClient.getId(), System.currentTimeMillis());
        this.clientInfos.put(simpleClient.getId(), simpleClient);
        this.focessUDPSocket.sendPacket(packet.getHost(), packet.getPort(), new ConnectedPacket(simpleClient.getId(), simpleClient.getToken()));
    }

    @PacketHandler
    public void onDisconnect(@NotNull final DisconnectPacket packet) {
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: client " + packet.getClientId() + " disconnect");
        if (this.clientInfos.get(packet.getClientId()) != null) {
            final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
            if (simpleClient.getToken().equals(packet.getToken())) {
                if (ASocket.isDebug())
                    System.out.println("P FocessSocket: server accept client " + packet.getClientId() + " disconnect");
                this.disconnect(packet.getClientId());
            } else if (ASocket.isDebug())
                System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " disconnect because of token conflict");
        } else if (ASocket.isDebug())
            System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " disconnect because of client not exist");
    }

    @PacketHandler
    public void onHeart(@NotNull final HeartPacket packet) {
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: client " + packet.getClientId() + " send heart");
        if (this.clientInfos.get(packet.getClientId()) != null) {
            final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
            if (simpleClient.getToken().equals(packet.getToken())) {
                if (ASocket.isDebug())
                    System.out.println("P FocessSocket: server accept client " + packet.getClientId() + " send heart");
                this.lastHeart.put(simpleClient.getId(), System.currentTimeMillis());
            } else if (ASocket.isDebug())
                System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " heart because of token conflict");
        } else if (ASocket.isDebug())
            System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " heart because of client not exist");
    }

    @PacketHandler
    public void onClientPacket(@NotNull final ClientPackPacket packet) {
        if (ASocket.isDebug())
            System.out.println("P FocessSocket: client " + packet.getClientId() + " send client packet");
        if (this.clientInfos.get(packet.getClientId()) != null) {
            final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
            if (simpleClient.getToken().equals(packet.getToken())) {
                if (ASocket.isDebug())
                    System.out.println("P FocessSocket: server accept client " + packet.getClientId() + " send client packet");
                for (final PackHandler packHandler : this.packHandlers.getOrDefault(simpleClient.getName(), Maps.newHashMap()).getOrDefault(packet.getPacket().getClass(), Lists.newArrayList()))
                    packHandler.handle(simpleClient.getId(), packet.getPacket());
            } else if (ASocket.isDebug())
                System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " send client packet because of token conflict");
        } else if (ASocket.isDebug())
            System.out.println("P FocessSocket: server reject client " + packet.getClientId() + " send client packet because of client not exist");
    }

    @Override
    public void sendPacket(final String client, final Packet packet) {
        for (final SimpleClient simpleClient : this.clientInfos.values())
            if (simpleClient.getName().equals(client))
                this.focessUDPSocket.sendPacket(Objects.requireNonNull(simpleClient.getHost()), simpleClient.getPort(), new ServerPackPacket(packet));
    }

}
