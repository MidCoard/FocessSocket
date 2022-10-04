package top.focess.net.socket;

import com.google.common.collect.Lists;
import top.focess.net.IllegalPortException;
import top.focess.net.PacketPreCodec;
import top.focess.net.SimpleClient;
import top.focess.net.packet.*;
import top.focess.net.receiver.Receiver;
import top.focess.net.receiver.ServerReceiver;
import top.focess.util.Pair;
import top.focess.util.RSA;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class FocessUDPSocket extends ASocket implements SendableSocket {

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
                    int packetId = packetPreCodec.readInt();
                    if (packetId == -1) {
                        SimpleClient client = this.getReceiver().getClient(packetPreCodec.readInt());
                        String encryptedData = packetPreCodec.readString();
                        if (client == null)
                            continue;
                        String data = RSA.decryptRSA(encryptedData, client.getPrivateKey());
                        packetPreCodec.clear();
                        packetPreCodec.push(data.getBytes(StandardCharsets.UTF_8));
                    }
                    packetPreCodec.reset();
                    Packet packet = packetPreCodec.readPacket();
                    if (isDebug())
                        System.out.println("P FocessSocket: receive packet: " + packet);
                    if (packet instanceof ClientPacket) {
                        if (packet instanceof SidedConnectPacket) {
                            final String name = ((SidedConnectPacket) packet).getName();
                            packet = new ConnectPacket(this.packet.getAddress().getHostName(), this.packet.getPort(), name, ((SidedConnectPacket) packet).isServerHeart(), ((SidedConnectPacket) packet).isEncrypt(), ((SidedConnectPacket) packet).getKey());
                        }
                        for (final Pair<Receiver, Method> pair : this.packetMethods.getOrDefault(packet.getClass(), Lists.newArrayList())) {
                            final Method method = pair.getValue();
                            try {
                                method.setAccessible(true);
                                final Object o = method.invoke(pair.getKey(), packet);
                                if (isDebug())
                                    System.out.println("P FocessSocket: send packet: " + o);
                                if (o instanceof ServerPacket) {
                                    final DatagramPacket sendPacket;
                                    final PacketPreCodec handler = new PacketPreCodec();
                                    handler.writePacket((Packet) o);
                                    if (this.getReceiver().getClient(((ClientPacket) packet).getClientId()).isEncrypt()) {
                                        byte[] bytes = RSA.encryptRSA(new String(handler.getBytes(), StandardCharsets.UTF_8), this.getReceiver().getClient(((ClientPacket) packet).getClientId()).getPublicKey()).getBytes(StandardCharsets.UTF_8);
                                        sendPacket = new DatagramPacket(bytes,bytes.length, this.packet.getSocketAddress());
                                    } else
                                        sendPacket = new DatagramPacket(handler.getBytes(), handler.getBytes().length, this.packet.getSocketAddress());
                                    this.socket.send(sendPacket);
                                }
                            } catch (final Exception ignored) {
                            }
                        }
                    }
                } catch (final Exception e) {
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
    public void close() {
        super.close();
        this.socket.close();
    }

    public boolean sendClientPacket(final String host, final int port, final ClientPacket packet) {
       throw new UnsupportedOperationException();
    }

    public boolean sendServerPacket(SimpleClient client, final String host, final int port, final ServerPacket packet) {
        final PacketPreCodec handler = new PacketPreCodec();
        if (isDebug())
            System.out.println("P FocessSocket: send packet: " + packet + " to " + host + ":" + port);
        if (handler.writePacket(packet)) {
            final DatagramPacket sendPacket;
            if (client.isEncrypt()) {
                String encryptedData = RSA.encryptRSA(new String(handler.getBytes(), StandardCharsets.UTF_8), client.getKey());
                sendPacket = new DatagramPacket(encryptedData.getBytes(StandardCharsets.UTF_8), encryptedData.getBytes(StandardCharsets.UTF_8).length, new InetSocketAddress(host, port));
            }
            else
                sendPacket = new DatagramPacket(handler.getBytes(), handler.getBytes().length, new InetSocketAddress(host, port));
            try {
                this.socket.send(sendPacket);
                return true;
            } catch (final IOException ignored) {
                return false;
            }
        }
        return false;
    }

    @Override
    public ServerReceiver getReceiver() {
        return (ServerReceiver)super.getReceiver();
    }
}
