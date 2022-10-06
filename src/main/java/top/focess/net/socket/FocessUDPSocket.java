package top.focess.net.socket;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import top.focess.net.IllegalPortException;
import top.focess.net.PacketPreCodec;
import top.focess.net.SimpleClient;
import top.focess.net.packet.*;
import top.focess.net.receiver.ClientReceiver;
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
import java.util.List;

public class FocessUDPSocket extends BothSideSocket {

    private final DatagramSocket socket;
    private final DatagramPacket packet;

    public FocessUDPSocket() throws IllegalPortException {
        this(0);
    }

    public FocessUDPSocket(final int port) throws IllegalPortException {
        try {
            this.socket = port == 0 ? new DatagramSocket() : new DatagramSocket(port);
            this.localPort = this.socket.getLocalPort();
        } catch (final SocketException e) {
            throw new IllegalPortException(port);
        }
        this.packet = new DatagramPacket(new byte[1024 * 1024], 1024 * 1024);
        final Thread thread = new Thread(() -> {
            while (!this.socket.isClosed()) {
                try {
                    this.socket.receive(this.packet);
                    final PacketPreCodec packetPreCodec = new PacketPreCodec();
                    if (this.isServerSide()) {
                        packetPreCodec.push(this.packet.getData());
                        int packetId = packetPreCodec.readInt();
                        if (packetId == -1) {
                            SimpleClient client = ((ServerReceiver) this.getReceiver()).getClient(packetPreCodec.readInt());
                            if (client == null || !client.isEncrypt())
                                continue;
                            String encryptedData = packetPreCodec.readString();
                            String data = RSA.decryptRSA(encryptedData, client.getPrivateKey());
                            packetPreCodec.clear();
                            packetPreCodec.push(data.getBytes(StandardCharsets.UTF_8));
                        } else packetPreCodec.reset();
                    } else if (this.isClientSide()) {
                        if (!((ClientReceiver) this.getReceiver()).isEncrypt())
                            packetPreCodec.push(this.packet.getData());
                        else
                            packetPreCodec.push(RSA.decryptRSA(new String(this.packet.getData(), StandardCharsets.UTF_8), ((ClientReceiver) this.getReceiver()).getPrivateKey()).getBytes(StandardCharsets.UTF_8));
                    }
                    Packet packet = packetPreCodec.readPacket();
                    if (isDebug())
                        System.out.println("S FocessSocket: receive packet: " + packet);
                    if (packet instanceof ClientPacket) {
                        if (packet instanceof SidedConnectPacket) {
                            final String name = ((SidedConnectPacket) packet).getName();
                            packet = new ConnectPacket(this.packet.getAddress().getHostName(), this.packet.getPort(), name, ((SidedConnectPacket) packet).isServerHeart(), ((SidedConnectPacket) packet).isEncrypt(), ((SidedConnectPacket) packet).getKey());
                        }
                        for (final Pair<Receiver, Method> pair : this.packetMethods.getOrDefault(packet.getClass(), Lists.newArrayList())) {
                            final Method method = pair.getValue();
                            try {
                                method.setAccessible(true);
                                method.invoke(pair.getKey(), packet);
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
    public void close() {
        super.close();
        this.socket.close();
    }

    public boolean sendClientPacket(final String host, final int port, final ClientPacket packet) {
        if (this.isServerSide())
            return false;
        if (!((ClientReceiver) this.getReceiver()).isConnected() && !(packet instanceof ConnectPacket) && !(packet instanceof SidedConnectPacket))
            return false;
        final PacketPreCodec packetPreCodec = new PacketPreCodec();
        if (isDebug())
            System.out.println("SC FocessSocket: send packet: " + packet);
        if (packetPreCodec.writePacket(packet)) {
            final DatagramPacket sendPacket;
            if (((ClientReceiver) this.getReceiver()).isEncrypt()) {
                PacketPreCodec codec = new PacketPreCodec();
                codec.writeInt(-1);
                codec.writeInt(((ClientReceiver) this.getReceiver()).getClientId());
                codec.writeString(RSA.encryptRSA(new String(packetPreCodec.getBytes(), StandardCharsets.UTF_8), ((ClientReceiver) this.getReceiver()).getKey()));
                sendPacket = new DatagramPacket(codec.getBytes(), codec.getBytes().length, new InetSocketAddress(host, port));
            } else sendPacket = new DatagramPacket(packetPreCodec.getBytes(), packetPreCodec.getBytes().length, new InetSocketAddress(host, port));
            try {
                this.socket.send(sendPacket);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    public boolean sendServerPacket(SimpleClient client, final String host, final int port, final ServerPacket packet) {
        if (this.isClientSide())
            return false;
        final PacketPreCodec packetPreCodec = new PacketPreCodec();
        if (isDebug())
            System.out.println("S FocessSocket: send packet: " + packet + " to " + host + ":" + port);
        if (packetPreCodec.writePacket(packet)) {
            final DatagramPacket sendPacket;
            if (client.isEncrypt()) {
                String encryptedData = RSA.encryptRSA(new String(packetPreCodec.getBytes(), StandardCharsets.UTF_8), client.getKey());
                sendPacket = new DatagramPacket(encryptedData.getBytes(StandardCharsets.UTF_8), encryptedData.getBytes(StandardCharsets.UTF_8).length, new InetSocketAddress(host, port));
            }
            else
                sendPacket = new DatagramPacket(packetPreCodec.getBytes(), packetPreCodec.length(), new InetSocketAddress(host, port));
            try {
                this.socket.send(sendPacket);
                return true;
            } catch (final IOException ignored) {
                return false;
            }
        }
        return false;
    }

}
