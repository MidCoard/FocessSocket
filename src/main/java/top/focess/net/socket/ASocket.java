package top.focess.net.socket;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import top.focess.net.PacketHandler;
import top.focess.net.packet.Packet;
import top.focess.net.receiver.Receiver;
import top.focess.util.Pair;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

public abstract class ASocket implements Socket {

    private static boolean debug = false;

    public static void enableDebug() {
        debug = true;
    }

    public static void disableDebug() {
        debug = false;
    }

    public static boolean isDebug() {
        return debug;
    }

    protected final Map<Class<? extends Packet>, List<Pair<Receiver, Method>>> packetMethods = Maps.newConcurrentMap();
    protected final List<Receiver> receivers = Lists.newArrayList();

    @Override
    public void registerReceiver(final Receiver receiver) {
        this.receivers.add(receiver);
        for (final Method method : receiver.getClass().getDeclaredMethods())
            if (method.getAnnotation(PacketHandler.class) != null)
                if (method.getParameterTypes().length == 1 && (method.getReturnType().equals(Void.TYPE) || Packet.class.isAssignableFrom(method.getReturnType()))) {
                    final Class<?> packetClass = method.getParameterTypes()[0];
                    if (Packet.class.isAssignableFrom(packetClass) && !Modifier.isAbstract(packetClass.getModifiers())) {
                        try {
                            this.packetMethods.compute((Class<? extends Packet>) packetClass, (k, v) -> {
                                if (v == null)
                                    v = Lists.newArrayList();
                                v.add(Pair.of(receiver, method));
                                return v;
                            });
                        } catch (final Exception ignored) {
                        }
                    }
                }
    }

    @Override
    public void unregister(Receiver receiver) {
        this.receivers.remove(receiver);
        this.packetMethods.values().forEach(list -> list.removeIf(pair -> pair.getKey().equals(receiver)));
    }

    @Override
    public void unregisterAll() {
        for (final Receiver receiver : this.receivers)
            receiver.unregisterAll();
        this.receivers.clear();
    }
}
