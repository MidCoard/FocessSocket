package top.focess.net.impl;

import com.google.common.collect.Lists;
import top.focess.net.IllegalPortException;
import top.focess.net.PacketPreCodec;
import top.focess.net.Receiver;
import top.focess.net.ServerReceiver;
import top.focess.net.packet.Packet;
import top.focess.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;

public class FocessSidedSocket extends ASocket {

    private final int localPort;
    private final ServerSocket server;

    public FocessSidedSocket(final int localPort) throws IllegalPortException {
        this.localPort = localPort;
        try {
            this.server = new ServerSocket(localPort);
        } catch (final IOException e) {
            throw new IllegalPortException(localPort);
        }
        final Thread thread = new Thread(() -> {
            while (!this.server.isClosed()) {
                try {
                    final java.net.Socket socket = this.server.accept();
                    final InputStream inputStream = socket.getInputStream();
                    final byte[] buffer = new byte[1024];
                    final PacketPreCodec packetPreCodec = new PacketPreCodec();
                    int length;
                    while ((length = inputStream.read(buffer)) != -1)
                        packetPreCodec.push(buffer, length);
                    final Packet packet = packetPreCodec.readPacket();
                    final OutputStream outputStream = socket.getOutputStream();
                    if (packet != null)
                        for (final Pair<Receiver, Method> pair : this.packetMethods.getOrDefault(packet.getClass(), Lists.newArrayList())) {
                            final Method method = pair.getValue();
                            try {
                                method.setAccessible(true);
                                final Object o = method.invoke(pair.getKey(), packet);
                                if (o != null) {
                                    final PacketPreCodec handler = new PacketPreCodec();
                                    handler.writePacket((Packet) o);
                                    outputStream.write(handler.getBytes());
                                    outputStream.flush();
                                }
                            } catch (final Exception ignored) {
                            }
                        }
                    socket.shutdownOutput();
                } catch (final IOException e) {
                    if (this.server.isClosed())
                        return;
                }
            }
        });
        thread.start();
    }

    @Override
    public void close() {
        for (final Receiver receiver : this.receivers)
            receiver.close();
        try {
            this.server.close();
        } catch (final IOException ignored) {
        }
    }

    @Override
    public void registerReceiver(final Receiver receiver) {
        if (!(receiver instanceof ServerReceiver))
            throw new UnsupportedOperationException();
        super.registerReceiver(receiver);
    }

    @Override
    public boolean containsServerSide() {
        return true;
    }

    @Override
    public boolean containsClientSide() {
        return false;
    }

    public int getLocalPort() {
        return this.localPort;
    }
}
