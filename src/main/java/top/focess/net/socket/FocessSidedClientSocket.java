package top.focess.net.socket;

import com.google.common.collect.Lists;
import top.focess.net.PacketPreCodec;
import top.focess.net.packet.Packet;
import top.focess.net.receiver.ClientReceiver;
import top.focess.net.receiver.FocessSidedClientReceiver;
import top.focess.net.receiver.Receiver;
import top.focess.util.Pair;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public class FocessSidedClientSocket extends ASocket {

    private final String host;
    private final int port;

    public FocessSidedClientSocket(final String host, final int port, String name, boolean serverHeart, boolean encrypt) {
        this.host = host;
        this.port = port;
        super.registerReceiver(new FocessSidedClientReceiver(this, name, serverHeart, encrypt));
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
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
            outputStream.write(packetPreCodec.getBytes());
            outputStream.flush();
            socket.shutdownOutput();
            final InputStream inputStream = socket.getInputStream();
            final byte[] buffer = new byte[1024];
            int length;
            final PacketPreCodec codec = new PacketPreCodec();
            while ((length = inputStream.read(buffer)) != -1)
                codec.push(buffer, length);
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

    public ClientReceiver getReceiver() {
        return (ClientReceiver) this.receivers.get(0);
    }
}
