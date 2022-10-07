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
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class FocessSocket extends BothSideSocket {

    private final ServerSocket server;

    public FocessSocket() throws IllegalPortException {
        this(0);
    }

    public FocessSocket(final int localPort) throws IllegalPortException {
        try {
            this.server = new ServerSocket(localPort);
            this.localPort = this.server.getLocalPort();
        } catch (final IOException e) {
            throw new IllegalPortException(localPort);
        }
        final Thread thread = new Thread(() -> {
            while (!this.server.isClosed())
                try {
                    final Socket socket = this.server.accept();
                    final InputStream inputStream = socket.getInputStream();
                    final byte[] buffer = new byte[1024];
                    final PacketPreCodec packetPreCodec = new PacketPreCodec();
                    int length;
                    if (this.isServerSide()) {
                        while ((length = inputStream.read(buffer)) != -1)
                            packetPreCodec.push(buffer, length);
                        int packetId = packetPreCodec.readInt();
                        if (packetId == -1) {
                            SimpleClient client = ((ServerReceiver) this.getReceiver()).getClient(packetPreCodec.readInt());
                            if (client == null || !client.isEncrypt()) {
                                inputStream.close();
                                continue;
                            }
                            byte[] encryptedData = packetPreCodec.readByteArray();
                            byte[] data = RSA.decryptRSA(encryptedData, client.getPrivateKey());
                            packetPreCodec.clear();
                            packetPreCodec.push(data);
                        } else
                            packetPreCodec.reset();
                    } else if (this.isClientSide()) {
                        if (!((ClientReceiver) this.getReceiver()).isEncrypt())
                            while ((length = inputStream.read(buffer)) != -1)
                                packetPreCodec.push(buffer, length);
                        else {
                            List<Byte> bytes = Lists.newArrayList();
                            while ((length = inputStream.read(buffer)) != -1)
                                for (int i = 0; i < length; i++)
                                    bytes.add(buffer[i]);
                            packetPreCodec.push(RSA.decryptRSA(Bytes.toArray(bytes), ((ClientReceiver) this.getReceiver()).getPrivateKey()));
                        }
                    }
                    inputStream.close();
                    final Packet packet = packetPreCodec.readPacket();
                    if (isDebug())
                        if (this.isServerSide())
                            System.out.println("S FocessSocket: receive packet: " + packet);
                        else if (this.isClientSide())
                            System.out.println("SC FocessSocket: receive packet: " + packet);
                    if (packet != null)
                        for (final Pair<Receiver, Method> pair : this.packetMethods.getOrDefault(packet.getClass(), Lists.newArrayList())) {
                            final Method method = pair.getValue();
                            try {
                                method.setAccessible(true);
                                method.invoke(pair.getKey(), packet);
                            } catch (final Exception ignored) {
                            }
                        }
                } catch (final Exception e) {
                    if (this.server.isClosed())
                        return;
                }
        });
        thread.start();
    }

    public boolean sendClientPacket(final String targetHost, final int targetPort, final ClientPacket packet) {
        if (this.isServerSide())
            return false;
        if (!((ClientReceiver) this.getReceiver()).isConnected() && !(packet instanceof ConnectPacket) && !(packet instanceof SidedConnectPacket))
            return false;
        final PacketPreCodec packetPreCodec = new PacketPreCodec();
        if (isDebug())
            System.out.println("SC FocessSocket: send packet: " + packet);
        if (packetPreCodec.writePacket(packet))
            try {
                final java.net.Socket socket = new java.net.Socket(targetHost, targetPort);
                final OutputStream outputStream = socket.getOutputStream();
                if (((ClientReceiver) this.getReceiver()).isEncrypt()) {
                    PacketPreCodec codec = new PacketPreCodec();
                    codec.writeInt(-1);
                    codec.writeInt(((ClientReceiver) this.getReceiver()).getClientId());
                    codec.writeByteArray(RSA.encryptRSA(packetPreCodec.getBytes(), ((ClientReceiver) this.getReceiver()).getKey()));
                    outputStream.write(codec.getBytes());
                } else
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

    public boolean sendServerPacket(SimpleClient client, String host, int port, ServerPacket packet){
        if (this.isClientSide())
            return false;
        final PacketPreCodec packetPreCodec = new PacketPreCodec();
        if (isDebug())
            System.out.println("S FocessSocket: send packet: " + packet + " to " + host + ":" + port);
        if (packetPreCodec.writePacket(packet))
            try {
                final java.net.Socket socket = new java.net.Socket(host, port);
                final OutputStream outputStream = socket.getOutputStream();
                if (client.isEncrypt())
                    outputStream.write(RSA.encryptRSA(packetPreCodec.getBytes(), client.getKey()));
                else
                    outputStream.write(packetPreCodec.getBytes());
                outputStream.flush();
                outputStream.close();
                return true;
            } catch (final IOException e) {
                return false;
            }
        return false;
    }
}
