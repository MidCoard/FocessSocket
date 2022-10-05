package top.focess.net.socket;

import com.google.common.collect.Lists;
import top.focess.net.IllegalPortException;
import top.focess.net.PacketPreCodec;
import top.focess.net.SimpleClient;
import top.focess.net.packet.ClientPacket;
import top.focess.net.packet.Packet;
import top.focess.net.packet.ServerPacket;
import top.focess.net.receiver.FocessSidedReceiver;
import top.focess.net.receiver.Receiver;
import top.focess.net.receiver.ServerReceiver;
import top.focess.util.Pair;
import top.focess.util.RSA;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

public class FocessSidedSocket extends top.focess.net.socket.ServerSocket {

    private final int localPort;
    private final ServerSocket server;

    public FocessSidedSocket(final int localPort) throws IllegalPortException {
        this.localPort = localPort;
        super.registerReceiver(new FocessSidedReceiver(this));
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
                    int packetId = packetPreCodec.readInt();
                    if (packetId == -1) {
                        SimpleClient client = this.getReceiver().getClient(packetPreCodec.readInt());
                        String encryptedData = packetPreCodec.readString();
                        if (client == null || !client.isEncrypt()) {
                            socket.shutdownOutput();
                            continue;
                        }
                        String data = RSA.decryptRSA(encryptedData, client.getPrivateKey());
                        packetPreCodec.clear();
                        packetPreCodec.push(data.getBytes(StandardCharsets.UTF_8));
                    }
                    packetPreCodec.reset();
                    final Packet packet = packetPreCodec.readPacket();
                    if (isDebug())
                        System.out.println("P FocessSocket: receive packet: " + packet);
                    final OutputStream outputStream = socket.getOutputStream();
                    if (packet instanceof ClientPacket)
                        for (final Pair<Receiver, Method> pair : this.packetMethods.getOrDefault(packet.getClass(), Lists.newArrayList())) {
                            final Method method = pair.getValue();
                            try {
                                method.setAccessible(true);
                                final Object o = method.invoke(pair.getKey(), packet);
                                if (isDebug())
                                    System.out.println("P FocessSocket: send packet: " + o);
                                if (o instanceof ServerPacket) {
                                    final PacketPreCodec handler = new PacketPreCodec();
                                    handler.writePacket((Packet) o);
                                    if (this.getReceiver().getClient(((ClientPacket) packet).getClientId()).isEncrypt())
                                        outputStream.write(RSA.encryptRSA(new String(handler.getBytes(), StandardCharsets.UTF_8), this.getReceiver().getClient(((ClientPacket) packet).getClientId()).getPublicKey()).getBytes(StandardCharsets.UTF_8));
                                    else
                                        outputStream.write(handler.getBytes());
                                    outputStream.flush();
                                }
                            } catch (final Exception ignored) {
                            }
                        }
                    socket.shutdownOutput();
                } catch (final Exception e) {
                    if (this.server.isClosed())
                        return;
                }
            }
        });
        thread.start();
    }

    @Override
    public void close() {
        super.close();
        try {
            this.server.close();
        } catch (final IOException ignored) {
        }
    }

    @Override
    public void registerReceiver(final Receiver receiver) {
        throw new UnsupportedOperationException();
    }

    public int getLocalPort() {
        return this.localPort;
    }

}
