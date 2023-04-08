package top.focess.net.receiver;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.focess.net.DisconnectedHandler;
import top.focess.net.PackHandler;
import top.focess.net.PacketHandler;
import top.focess.net.SimpleClient;
import top.focess.net.packet.ClientPackPacket;
import top.focess.net.packet.HeartPacket;
import top.focess.net.packet.Packet;
import top.focess.net.packet.ServerHeartPacket;
import top.focess.net.socket.ASocket;
import top.focess.net.socket.Socket;
import top.focess.scheduler.FocessScheduler;
import top.focess.scheduler.Scheduler;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AServerReceiver implements ServerReceiver {


    protected final Map<Integer, Long> lastHeart = Maps.newConcurrentMap();
    protected final Map<Integer, SimpleClient> clientInfos = Maps.newConcurrentMap();
    protected final Map<String, Map<Class<?>, List<PackHandler>>> packHandlers = Maps.newConcurrentMap();
    protected final Scheduler scheduler;
    protected final Socket socket;
    protected final AtomicInteger defaultClientId = new AtomicInteger(0);
    private DisconnectedHandler disconnectedHandler;

    public AServerReceiver(Socket socket) {
        this.socket = socket;
        this.scheduler = new FocessScheduler("FocessServerReceiver");
        this.scheduler.runTimer(() -> {
            for (final SimpleClient simpleClient : this.clientInfos.values()) {
                final long time = this.lastHeart.getOrDefault(simpleClient.getId(), 0L);
                if (System.currentTimeMillis() - time > 10 * 1000)
                    this.disconnect(simpleClient.getId());
            }
        }, Duration.ZERO, Duration.ofSeconds(1));
        this.scheduler.runTimer(() -> {
            if (this.isServerSide())
                for (SimpleClient client : this.clientInfos.values())
                    if (client.isServerHeart())
                        this.sendPacket(client.getId(), new ServerHeartPacket(System.currentTimeMillis()));
        }, Duration.ZERO, Duration.ofSeconds(2));
    }

    @NotNull
    protected static String generateToken() {
        final StringBuilder stringBuilder = new StringBuilder();
        final Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < 64; i++) {
            switch (random.nextInt(3)) {
                case 0:
                    stringBuilder.append((char) ('0' + random.nextInt(10)));
                    break;
                case 1:
                    stringBuilder.append((char) ('a' + random.nextInt(26)));
                    break;
                case 2:
                    stringBuilder.append((char) ('A' + random.nextInt(26)));
                    break;
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean isConnected(final String client) {
        return this.clientInfos.values().stream().anyMatch(simpleClient -> simpleClient.getName().equals(client));
    }

    @Override
    public @Nullable SimpleClient getClient(final String name) {
        return this.clientInfos.values().stream().filter(simpleClient -> simpleClient.getName().equals(name)).findFirst().orElse(null);
    }

    @Override
    public void unregisterAll() {
        this.packHandlers.clear();
    }

    @Override
    public <T extends Packet> void register(final String name, final Class<T> c, final PackHandler<T> packHandler) {
        this.packHandlers.compute(name, (k1, v1) -> {
            if (v1 == null)
                v1 = Maps.newHashMap();
            v1.compute(c, (k2, v2) -> {
                if (v2 == null)
                    v2 = Lists.newArrayList();
                v2.add(packHandler);
                return v2;
            });
            return v1;
        });
    }

    @Override
    public void unregister(PackHandler handler) {
        this.packHandlers.values().forEach(v -> v.values().forEach(v1 -> v1.remove(handler)));
    }

    @Override
    public void disconnect(String client) {
        for (final SimpleClient simpleClient : this.clientInfos.values())
            if (simpleClient.getName().equals(client))
                this.disconnect(simpleClient.getId());
    }

    @PacketHandler
    public void onHeart(@NotNull final HeartPacket packet) {
        synchronized (this) {
            if (this.clientInfos.get(packet.getClientId()) == null) {
                if (ASocket.isDebug())
                    System.out.println("S FocessSocket " + this + ": server reject client " + packet.getClientId() + " sending heart because of client not exist");
                return;
            }
        }
        final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
        if (simpleClient.getToken().equals(packet.getToken()) && System.currentTimeMillis() + 5 * 1000 > packet.getTime()) {
            if (ASocket.isDebug())
                System.out.println("S FocessSocket " + this + ": server accept client " + packet.getClientId() + " sending heart");
            this.lastHeart.put(simpleClient.getId(), packet.getTime());
            return;
        }
        if (!simpleClient.getToken().equals(packet.getToken())) {
            if (ASocket.isDebug())
                System.out.println("S FocessSocket " + this + ": server reject client " + packet.getClientId() + " sending heart because of token conflict");
            return;
        }
        if (ASocket.isDebug())
            System.out.println("S FocessSocket " + this + ": client " + packet.getClientId() + " sending heart");
    }

    @PacketHandler
    public void onClientPacket(@NotNull final ClientPackPacket packet) {
        if (this.clientInfos.get(packet.getClientId()) == null) {
            if (ASocket.isDebug())
                System.out.println("S FocessSocket " + this + ": server reject server receiving client packet from client " + packet.getClientId() + " because of client not exist");
            return;
        }
        final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
        if (simpleClient.getToken().equals(packet.getToken())) {
            if (ASocket.isDebug())
                System.out.println("S FocessSocket " + this + ": server accept server receiving client packet from client " + packet.getClientId() + " ");
            for (final PackHandler packHandler : this.packHandlers.getOrDefault(simpleClient.getName(), Maps.newHashMap()).getOrDefault(packet.getPacket().getClass(), Lists.newArrayList()))
                packHandler.handle(simpleClient.getId(), packet.getPacket());
        } else if (ASocket.isDebug())
            System.out.println("S FocessSocket " + this + ": server reject server receiving client packet from client " + packet.getClientId() + "  because of token conflict");
    }

    @Override
    public void close() {
        this.scheduler.close();
        for (final Integer id : this.clientInfos.keySet())
            this.disconnect(id);
        this.clientInfos.clear();
        this.unregisterAll();
    }

    @Override
    public SimpleClient getClient(int id) {
        return this.clientInfos.get(id);
    }

    @Override
    public List<SimpleClient> getClients() {
        return Lists.newArrayList(this.clientInfos.values());
    }

    public void disconnect(final int clientId) {
        this.clientInfos.remove(clientId);
        if (this.disconnectedHandler != null)
            this.disconnectedHandler.handle(clientId);
    }

    @Override
    public void setDisconnectedHandler(DisconnectedHandler disconnectedHandler) {
        this.disconnectedHandler = disconnectedHandler;
    }
}
