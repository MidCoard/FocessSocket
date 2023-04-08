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
import top.focess.scheduler.ThreadPoolScheduler;
import top.focess.util.Pair;
import top.focess.util.RSA;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class FocessUDPSocket extends BothSideSocket {

    private final DatagramSocket socket;
    private final ThreadPoolScheduler scheduler = new ThreadPoolScheduler(10, false, "FocessUDPSocket", true);

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
        final Thread thread = new Thread(() -> {
            while (!this.socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[1024 * 1024], 1024 * 1024);
                    // always gc
                    this.socket.receive(packet);
                    scheduler.run(() -> handle(packet));
                    while (scheduler.getRemainingTasks().size() > 20);
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
        this.scheduler.close();
        this.socket.close();
    }

    public boolean sendClientPacket(final String host, final int port, final ClientPacket packet) {
        if (this.isServerSide())
            return false;
        if (!((ClientReceiver) this.getReceiver()).isConnected() && !(packet instanceof ConnectPacket) && !(packet instanceof SidedConnectPacket))
            return false;
        final PacketPreCodec packetPreCodec = new PacketPreCodec();
        if (isDebug())
            System.out.println("UC FocessSocket: client send packet: " + packet + " from localhost:" + this.localPort + " to " + host + ":" + port );
        if (packetPreCodec.writePacket(packet)) {
            final DatagramPacket sendPacket;
            if (((ClientReceiver) this.getReceiver()).isEncrypt() && !(packet instanceof SidedConnectPacket) && !(packet instanceof ConnectPacket)) {
                PacketPreCodec codec = new PacketPreCodec();
                codec.writeInt(-1);
                codec.writeInt(((ClientReceiver) this.getReceiver()).getClientId());
                codec.writeByteArray(RSA.encryptRSA(packetPreCodec.getBytes(), ((ClientReceiver) this.getReceiver()).getKey()));
                sendPacket = new DatagramPacket(codec.getBytes(), codec.getBytes().length, new InetSocketAddress(host, port));
            } else
                sendPacket = new DatagramPacket(packetPreCodec.getBytes(), packetPreCodec.getBytes().length, new InetSocketAddress(host, port));
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
            System.out.println("US FocessSocket: server send packet: " + packet + " from localhost:" + this.localPort + " to " + host + ":" + port);
        if (packetPreCodec.writePacket(packet)) {
            final DatagramPacket sendPacket;
            if (client.isEncrypt()) {
                byte[] encryptedData = RSA.encryptRSA(packetPreCodec.getBytes(), client.getKey());
                sendPacket = new DatagramPacket(encryptedData, encryptedData.length, new InetSocketAddress(host, port));
            } else
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
    
    private void handle(DatagramPacket p) {
        try {
            final PacketPreCodec packetPreCodec = new PacketPreCodec();
            if (this.isServerSide()) {
                packetPreCodec.push(p.getData(), p.getOffset(), p.getLength());
                int packetId = packetPreCodec.readInt();
                if (packetId == -1) {
                    SimpleClient client = ((ServerReceiver) this.getReceiver()).getClient(packetPreCodec.readInt());
                    if (client == null || !client.isEncrypt())
                        return;
                    byte[] data = RSA.decryptRSA(packetPreCodec.readByteArray(), client.getPrivateKey());
                    packetPreCodec.clear();
                    packetPreCodec.push(data);
                } else packetPreCodec.reset();
            } else if (this.isClientSide()) {
                if (!((ClientReceiver) this.getReceiver()).isEncrypt())
                    packetPreCodec.push(p.getData(), p.getOffset(), p.getLength());
                else {
                    byte[] data = Bytes.toArray(Bytes.asList(p.getData()).subList(p.getOffset(), p.getOffset() + p.getLength()));
                    packetPreCodec.push(RSA.decryptRSA(data, ((ClientReceiver) this.getReceiver()).getPrivateKey()));
                }
            }
            Packet packet = packetPreCodec.readPacket();
            if (isDebug())
                if (this.isServerSide())
                    System.out.println("US FocessSocket: server receive packet: " + packet + " from " + p.getAddress().getHostName() + ":" + p.getPort() + " to localhost:" + this.localPort);
                else if (this.isClientSide())
                    System.out.println("UC FocessSocket: client receive packet: " + packet + " from " + p.getAddress().getHostName() + ":" + p.getPort() + " to localhost:" + this.localPort);
            if (packet instanceof SidedConnectPacket) {
                final String name = ((SidedConnectPacket) packet).getName();
                packet = new ConnectPacket(p.getAddress().getHostName(), p.getPort(), name, ((SidedConnectPacket) packet).isServerHeart(), ((SidedConnectPacket) packet).isEncrypt(), ((SidedConnectPacket) packet).getKey());
            }
            for (final Pair<Receiver, Method> pair : this.packetMethods.getOrDefault(packet.getClass(), Lists.newArrayList())) {
                final Method method = pair.getValue();
                try {
                    method.setAccessible(true);
                    method.invoke(pair.getKey(), packet);
                } catch (final Exception ignored) {
                }
            }
        } catch (Exception ignored) {
            
        }
    }

}
