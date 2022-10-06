package top.focess.net.receiver;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import top.focess.net.PackHandler;
import top.focess.net.packet.DisconnectPacket;
import top.focess.net.packet.Packet;
import top.focess.util.RSA;
import top.focess.util.RSAKeypair;

import java.util.List;
import java.util.Map;

public abstract class AClientReceiver implements ClientReceiver {

    protected final String host;
    protected final int port;
    protected final String name;
    protected final Map<Class<?>, List<PackHandler>> packHandlers = Maps.newConcurrentMap();
    protected final boolean serverHeart;
    protected final boolean encrypt;
    protected String token;
    protected int id;
    protected volatile boolean connected;

    protected final RSAKeypair keypair;

    protected String key;

    public AClientReceiver(final String host, final int port, final String name, final boolean serverHeart, final boolean encrypt) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.serverHeart = serverHeart;
        this.encrypt = encrypt;
        if (this.encrypt)
            keypair = RSA.genRSAKeypair();
        else keypair = new RSAKeypair(null, null);
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
        this.sendPacket(new DisconnectPacket(this.id, this.token));
    }

    @Override
    public void waitConnected() {
        while (!this.connected) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
