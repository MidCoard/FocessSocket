package top.focess.net.receiver;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.focess.net.Client;
import top.focess.net.PackHandler;
import top.focess.net.SimpleClient;
import top.focess.net.packet.Packet;

import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class AServerReceiver implements ServerReceiver {


    protected final Map<Integer, Long> lastHeart = Maps.newConcurrentMap();
    protected final Map<Integer, SimpleClient> clientInfos = Maps.newConcurrentMap();
    protected final Map<String, Map<Class<?>, List<PackHandler>>> packHandlers = Maps.newConcurrentMap();
    protected int defaultClientId;

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
    public @Nullable Client getClient(final String name) {
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

}
