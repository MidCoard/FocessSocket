package top.focess.net.receiver;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import top.focess.net.DisconnectedHandler;
import top.focess.net.PackHandler;
import top.focess.net.PacketHandler;
import top.focess.net.packet.ConnectedPacket;
import top.focess.net.packet.Packet;
import top.focess.net.packet.ServerHeartPacket;
import top.focess.net.packet.ServerPackPacket;
import top.focess.net.socket.ASocket;
import top.focess.scheduler.FocessScheduler;
import top.focess.util.RSA;
import top.focess.util.RSAKeypair;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public abstract class AClientReceiver implements ClientReceiver {

    protected final String host;
    protected final int port;
    protected final String name;
    protected final Map<Class<?>, List<PackHandler>> packHandlers = Maps.newConcurrentMap();
    protected final boolean serverHeart;
    protected final boolean encrypt;
    protected final FocessScheduler scheduler;
    protected final RSAKeypair keypair;
    protected String token;
    protected int id;
    protected volatile boolean connected;
    protected long lastHeart;
    protected String key;
    protected DisconnectedHandler disconnectedHandler;

    private final Object waitLock = new Object();

    public AClientReceiver(FocessScheduler scheduler, final String host, final int port, final String name, final boolean serverHeart, final boolean encrypt) {
        this.scheduler = scheduler;
        this.host = host;
        this.port = port;
        this.name = name;
        this.serverHeart = serverHeart;
        this.encrypt = encrypt;
        if (this.encrypt)
            keypair = RSA.genRSAKeypair();
        else keypair = new RSAKeypair(null, null);
        if (this.serverHeart)
            this.scheduler.runTimer(() -> {
                if (this.connected)
                    if (System.currentTimeMillis() - this.lastHeart > 10 * 1000)
                        this.disconnect();
            }, Duration.ZERO, Duration.ofSeconds(1));
    }

    @Override
    public <T extends Packet> void register(final Class<T> c, final PackHandler<T> packHandler) {
        this.packHandlers.compute(c, (k, v) -> {
            if (v == null)
                v = Lists.newArrayList();
            v.add(packHandler);
            return v;
        });
    }

    @Override
    public void unregisterAll() {
        this.packHandlers.clear();
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    @Override
    public boolean isConnected() {
        return this.connected;
    }

    @Override
    public int getClientId() {
        return this.id;
    }

    @Override
    public String getClientToken() {
        return this.token;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void unregister(PackHandler handler) {
        this.packHandlers.values().forEach(v -> v.remove(handler));
    }

    @Override
    public boolean isServerHeart() {
        return this.serverHeart;
    }

    @Override
    public boolean isEncrypt() {
        return this.encrypt;
    }

    @Override
    public String getPrivateKey() {
        return this.keypair.getPrivateKey();
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void disconnect() {
        this.connected = false;
        if (this.disconnectedHandler != null)
            this.disconnectedHandler.handle();
    }

    @Override
    public boolean waitConnected() {
        synchronized (this.waitLock) {
            if (this.connected)
                return true;
            try {
                this.waitLock.wait();
            } catch (InterruptedException e) {
                return false;
            }
        }
        return this.connected;
    }

    @PacketHandler
    public synchronized void onConnected(final ConnectedPacket packet) {
        if (this.connected) {
            if (ASocket.isDebug())
                System.out.println("C FocessSocket: client reject client " + this.name + " connect from " + this.host + ":" + this.port + " because of already connected");
            return;
        }
        if (ASocket.isDebug())
            System.out.println("C FocessSocket: server accept client " + this.name + " connect from " + this.host + ":" + this.port);
        this.token = packet.getToken();
        this.id = packet.getClientId();
        this.key = packet.getKey();
        this.lastHeart = System.currentTimeMillis();
        this.connected = true;
        synchronized (this.waitLock) {
            this.waitLock.notifyAll();
        }
    }

    @PacketHandler
    public synchronized void onServerPacket(final ServerPackPacket packet) {
        if (!this.connected) {
            if (ASocket.isDebug())
                System.out.println("C FocessSocket: client reject client " + this.name + " receive packet from " + this.host + ":" + this.port + " because of not connected");
            return;
        }
        if (ASocket.isDebug())
            System.out.println("C FocessSocket: client accept client " + this.name + " receive packet from " + this.host + ":" + this.port);
        for (final PackHandler packHandler : this.packHandlers.getOrDefault(packet.getPacket().getClass(), Lists.newArrayList()))
            packHandler.handle(this.id, packet.getPacket());
    }

    @PacketHandler
    public synchronized void onServerHeart(final ServerHeartPacket packet) {
        if (!this.connected) {
            if (ASocket.isDebug())
                System.out.println("C FocessSocket: client reject server " + this.name + " send heart from " + this.host + ":" + this.port + " because of not connected");
            return;
        }
        if (ASocket.isDebug())
            System.out.println("C FocessSocket: client accept server " + this.name + " send heart from " + this.host + ":" + this.port);
        this.lastHeart = System.currentTimeMillis();
    }

    @Override
    public void setDisconnectedHandler(DisconnectedHandler disconnectedHandler) {
        this.disconnectedHandler = disconnectedHandler;
    }
}
