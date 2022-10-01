package top.focess.net.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import top.focess.net.ClientReceiver;
import top.focess.net.PackHandler;
import top.focess.net.packet.Packet;

import java.util.List;
import java.util.Map;

public abstract class AClientReceiver implements ClientReceiver {

    protected final String host;
    protected final int port;
    protected final String name;
    protected final Map<Class<?>, List<PackHandler>> packHandlers = Maps.newConcurrentMap();
    protected String token;
    protected int id;
    protected volatile boolean connected;

    public AClientReceiver(final String host, final int port, final String name) {
        this.host = host;
        this.port = port;
        this.name = name;
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

}
