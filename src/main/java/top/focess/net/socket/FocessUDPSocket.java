package top.focess.net.socket;

import com.google.common.collect.Lists;
import top.focess.net.IllegalPortException;
import top.focess.net.PacketPreCodec;
import top.focess.net.packet.ConnectPacket;
import top.focess.net.packet.Packet;
import top.focess.net.packet.SidedConnectPacket;
import top.focess.net.receiver.Receiver;
import top.focess.net.receiver.ServerReceiver;
import top.focess.util.Pair;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class FocessUDPSocket extends ASocket {

    private final DatagramSocket socket;
    private final DatagramPacket packet;

    public FocessUDPSocket(final int port) throws IllegalPortException {
        try {
            this.socket = new DatagramSocket(port);
        } catch (final SocketException e) {
            throw new IllegalPortException(port);
        }
        this.packet = new DatagramPacket(new byte[1024 * 1024], 1024 * 1024);
        final Thread thread = new Thread(() -> {
            while (!this.socket.isClosed()) {
                try {
                    this.socket.receive(this.packet);
                    final PacketPreCodec packetPreCodec = new PacketPreCodec();
                    packetPreCodec.push(this.packet.getData(), this.packet.getOffset(), this.packet.getLength());
                    Packet packet = packetPreCodec.readPacket();
                    if (packet != null) {
                        if (packet instanceof SidedConnectPacket) {
                            final String name = ((SidedConnectPacket) packet).getName();
                            packet = new ConnectPacket(this.packet.getAddress().getHostName(), this.packet.getPort(), name);
                        }
                        for (final Pair<Receiver, Method> pair : this.packetMethods.getOrDefault(packet.getClass(), Lists.newArrayList())) {
                            final Method method = pair.getValue();
                            try {
                                method.setAccessible(true);
                                final Object o = method.invoke(pair.getKey(), packet);
                                if (o != null) {
                                    final PacketPreCodec handler = new PacketPreCodec();
                                    handler.writePacket((Packet) o);
                                    final DatagramPacket sendPacket = new DatagramPacket(handler.getBytes(), handler.getBytes().length, this.packet.getSocketAddress());
                                    this.socket.send(sendPacket);
                                }
                            } catch (final Exception ignored) {
                            }
                        }
                    }
                } catch (final IOException e) {
                    if (this.socket.isClosed())
                        return;
                }
            }
        });
        thread.start();
    }

    @Override
    public void registerReceiver(final Receiver receiver) {
        if (!(receiver instanceof ServerReceiver))
            throw new UnsupportedOperationException();
        super.registerReceiver(receiver);
    }

    @Override
    public boolean containsServerSide() {
        return this.receivers.size() != 0;
    }

    @Override
    public boolean containsClientSide() {
        return false;
    }

    @Override
    public void close() {
        for (final Receiver receiver : this.receivers)
            receiver.close();
        this.socket.close();
    }

    public void sendPacket(final String host, final int port, final Packet packet) {
        final PacketPreCodec handler = new PacketPreCodec();
        handler.writePacket(packet);
        final DatagramPacket sendPacket = new DatagramPacket(handler.getBytes(), handler.getBytes().length, new InetSocketAddress(host, port));
        try {
            this.socket.send(sendPacket);
        } catch (final IOException ignored) {
        }
    }
}
