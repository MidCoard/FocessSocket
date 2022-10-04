package top.focess.net.socket;

import com.google.common.collect.Lists;
import top.focess.net.IllegalPortException;
import top.focess.net.PacketPreCodec;
import top.focess.net.packet.Packet;
import top.focess.net.receiver.Receiver;
import top.focess.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;

public class FocessSocket extends ASocket implements SendableSocket {

    private final ServerSocket server;
    private final int localPort;

    public FocessSocket(final int localPort) throws IllegalPortException {
        this.localPort = localPort;
        try {
            this.server = new ServerSocket(localPort);
        } catch (final IOException e) {
            throw new IllegalPortException(localPort);
        }
        final Thread thread = new Thread(() -> {
            while (!this.server.isClosed())
                try {
                    final java.net.Socket socket = this.server.accept();
                    final InputStream inputStream = socket.getInputStream();
                    final byte[] buffer = new byte[1024];
                    final PacketPreCodec packetPreCodec = new PacketPreCodec();
                    int length;
                    while ((length = inputStream.read(buffer)) != -1)
                        packetPreCodec.push(buffer, length);
                    inputStream.close();
                    final Packet packet = packetPreCodec.readPacket();
                    if (isDebug())
                        System.out.println("S FocessSocket: receive packet: " + packet);
                    if (packet != null)
                        for (final Pair<Receiver, Method> pair : this.packetMethods.getOrDefault(packet.getClass(), Lists.newArrayList())) {
                            final Method method = pair.getValue();
                            try {
                                method.setAccessible(true);
                                method.invoke(pair.getKey(), packet);
                            } catch (final Exception ignored) {
                            }
                        }
                } catch (final IOException e) {
                    if (this.server.isClosed())
                        return;
                }
        });
        thread.start();
    }

    public boolean sendPacket(final String targetHost, final int targetPort, final Packet packet) {
        final PacketPreCodec packetPreCodec = new PacketPreCodec();
        if (isDebug())
            System.out.println("S FocessSocket: send packet: " + packet);
        if (packetPreCodec.writePacket(packet))
            try {
                final java.net.Socket socket = new java.net.Socket(targetHost, targetPort);
                final OutputStream outputStream = socket.getOutputStream();
                outputStream.write(packetPreCodec.getBytes());
                outputStream.flush();
                outputStream.close();
                return true;
            } catch (final IOException e) {
                return false;
            }
        return false;
    }

    @Override
    public void close() {
        super.close();
        try {
            this.server.close();
        } catch (final IOException ignored) {
        }
    }

    public int getLocalPort() {
        return this.localPort;
    }

}
