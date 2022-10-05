package top.focess.net.socket;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import top.focess.net.PacketPreCodec;
import top.focess.net.packet.Packet;
import top.focess.net.receiver.FocessUDPClientReceiver;
import top.focess.net.receiver.Receiver;
import top.focess.util.Pair;
import top.focess.util.RSA;

import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FocessUDPClientSocket extends ClientSocket{

    private DatagramSocket socket;

    public FocessUDPClientSocket(final String host, final int port, String name, boolean serverHeart, boolean encrypt) {
        super(host, port);
        super.registerReceiver(new FocessUDPClientReceiver(this, name, serverHeart, encrypt));
    }

    private DatagramSocket getSocket() {
        if (this.socket == null || this.socket.isClosed())
            try {
                this.socket = new DatagramSocket();
            } catch (SocketException ignored) {
                throw new IllegalStateException("Cannot create DatagramSocket");
            }
        return socket;
    }

    public boolean sendPacket(Packet packet) {
        if (isDebug())
            System.out.println("PC FocessSocket: send packet: " + packet);
        final PacketPreCodec packetPreCodec = new PacketPreCodec();
        try {
            if (!packetPreCodec.writePacket(packet))
                return false;
            InetSocketAddress address = new InetSocketAddress(this.getHost(), this.getPort());
            if (this.getReceiver().isEncrypt()) {
                PacketPreCodec codec = new PacketPreCodec();
                codec.writeInt(-1);
                codec.writeInt(this.getReceiver().getClientId());
                codec.writeString(RSA.encryptRSA(new String(packetPreCodec.getBytes(), StandardCharsets.UTF_8),this.getReceiver().getKey()));
                getSocket().send(new DatagramPacket(codec.getBytes(),codec.length(),address));
            } else
                getSocket().send(new DatagramPacket(packetPreCodec.getBytes(), packetPreCodec.length(), address));
            DatagramPacket datagramPacket = new DatagramPacket(new byte[1024 * 1024], 1024 * 1024);
            this.socket.receive(datagramPacket);
            final PacketPreCodec codec = new PacketPreCodec();
            if (!this.getReceiver().isEncrypt())
                codec.push(datagramPacket.getData(), datagramPacket.getOffset(),  datagramPacket.getLength());
            else {
                List<Byte> bytes = Lists.newArrayList();
                for (int i = 0; i < datagramPacket.getLength(); i++)
                    bytes.add(datagramPacket.getData()[i + datagramPacket.getOffset()]);
                codec.push(RSA.decryptRSA(new String (Bytes.toArray(bytes), StandardCharsets.UTF_8), this.getReceiver().getPrivateKey()).getBytes(StandardCharsets.UTF_8));
            }
            final Packet p = codec.readPacket();
            if (isDebug())
                System.out.println("PC FocessSocket: receive packet: " + p);
            if (p != null)
                for (final Pair<Receiver, Method> pair : this.packetMethods.getOrDefault(p.getClass(), Lists.newArrayList())) {
                    final Method method = pair.getValue();
                    try {
                        method.setAccessible(true);
                        method.invoke(pair.getKey(), p);
                    } catch (final Exception ignored) {
                    }
                }
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public void close() {
        super.close();
        this.socket.close();
    }
}
