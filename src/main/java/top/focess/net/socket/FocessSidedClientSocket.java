package top.focess.net.socket;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import top.focess.net.PacketPreCodec;
import top.focess.net.packet.Packet;
import top.focess.net.packet.SidedConnectPacket;
import top.focess.net.receiver.FocessSidedClientReceiver;
import top.focess.net.receiver.Receiver;
import top.focess.util.Pair;
import top.focess.util.RSA;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

public class FocessSidedClientSocket extends ClientSocket {

    public FocessSidedClientSocket(final String host, final int port, String name, boolean serverHeart, boolean encrypt, Duration sendDuration) {
        super(host, port);
        super.registerReceiver(new FocessSidedClientReceiver(this, name, serverHeart, encrypt, sendDuration));
    }


    public FocessSidedClientSocket(final String host, final int port, String name, boolean serverHeart, boolean encrypt) {
        this(host, port, name, serverHeart, encrypt, Duration.ofMillis(100));
    }

    public FocessSidedClientSocket(final String host, final int port, String name) {
        this(host, port, name, false, false);
    }


    public <T extends Packet> boolean sendPacket(final T packet) {
        if (isDebug())
            System.out.println("PC FocessSocket: send packet: " + packet);
        final PacketPreCodec packetPreCodec = new PacketPreCodec();
        try {
            if (!packetPreCodec.writePacket(packet))
                return false;
            final java.net.Socket socket = new java.net.Socket(this.host, this.port);
            final OutputStream outputStream = socket.getOutputStream();
            if (this.getReceiver().isEncrypt() && !(packet instanceof SidedConnectPacket)) {
                PacketPreCodec codec = new PacketPreCodec();
                codec.writeInt(-1);
                codec.writeInt(this.getReceiver().getClientId());
                codec.writeByteArray(RSA.encryptRSA(packetPreCodec.getBytes(), this.getReceiver().getKey()));
                outputStream.write(codec.getBytes());
            } else
                outputStream.write(packetPreCodec.getBytes());
            outputStream.flush();
            socket.shutdownOutput();
            final InputStream inputStream = socket.getInputStream();
            final byte[] buffer = new byte[1024];
            int length;
            final PacketPreCodec codec = new PacketPreCodec();
            if (!this.getReceiver().isEncrypt())
                while ((length = inputStream.read(buffer)) != -1)
                    codec.push(buffer, length);
            else {
                List<Byte> bytes = Lists.newArrayList();
                while ((length = inputStream.read(buffer)) != -1)
                    for (int i = 0; i < length; i++)
                        bytes.add(buffer[i]);
                codec.push(RSA.decryptRSA(Bytes.toArray(bytes), this.getReceiver().getPrivateKey()));
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

    public void registerReceiver(final Receiver receiver) {
        throw new UnsupportedOperationException();
    }

}
