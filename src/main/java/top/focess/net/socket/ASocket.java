package top.focess.net.socket;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
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
    protected final Map<Class<? extends Packet>, List<Pair<Receiver, Method>>> packetMethods = Maps.newConcurrentMap();
    protected final List<Receiver> receivers = Lists.newArrayList();

    public static void enableDebug() {
        debug = true;
    }

    public static void disableDebug() {
        debug = false;
    }

    public static boolean isDebug() {
        return debug;
    }

    private static @NotNull List<Method> getAllMethods(Class<?> c) {
        List<Method> methods = Lists.newArrayList();
        List<String> methodNames = Lists.newArrayList();
        while (c != null) {
            for (Method method : c.getDeclaredMethods())
                if (!methodNames.contains(method.getName())) {
                    methods.add(method);
                    methodNames.add(method.getName());
                }
            c = c.getSuperclass();
        }
        return methods;
    }

    @Override
    public void registerReceiver(final Receiver receiver) {
        if (this.receivers.size() > 0)
            throw new UnsupportedOperationException();
        this.receivers.add(receiver);
        for (final Method method : getAllMethods(receiver.getClass()))
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
    public void unregister(@NotNull Receiver receiver) {
        receiver.close();
        this.receivers.remove(receiver);
        this.packetMethods.values().forEach(list -> list.removeIf(pair -> pair.getKey().equals(receiver)));
    }

    @Override
    public void unregisterAll() {
        for (final Receiver receiver : this.receivers)
            receiver.close();
        this.receivers.clear();
        this.packetMethods.clear();
    }

    @Override
    public void close() {
        for (final Receiver receiver : this.receivers)
            receiver.close();
    }

    @Override
    public boolean isClientSide() {
        return this.receivers.stream().anyMatch(Receiver::isClientSide);
    }

    @Override
    public boolean isServerSide() {
        return this.receivers.stream().anyMatch(Receiver::isServerSide);
    }

    public Receiver getReceiver() {
        if (this.receivers.isEmpty())
            return null;
        return this.receivers.get(0);
    }
}
