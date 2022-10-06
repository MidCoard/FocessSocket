package top.focess.net.receiver;

import top.focess.net.packet.*;
import top.focess.net.socket.BothSideSocket;
import top.focess.net.socket.FocessSocket;
import top.focess.net.socket.FocessUDPSocket;
import top.focess.scheduler.FocessScheduler;

import java.time.Duration;

public class FocessClientReceiver extends AClientReceiver {
    private final BothSideSocket socket;

    public FocessClientReceiver(final BothSideSocket socket, final String localhost, final String host, final int port, final String name, final boolean serverHeart, final boolean encrypt) {
        super(new FocessScheduler("FocessClientReceiver"), host, port, name, serverHeart, encrypt);
        this.socket = socket;
        this.scheduler.runTimer(() -> {
            if (this.connected)
                this.socket.sendClientPacket(host, port, new HeartPacket(this.id, this.token, System.currentTimeMillis()));
            else
                this.socket.sendClientPacket(this.host, this.port, new ConnectPacket(localhost, this.socket.getLocalPort(), name, serverHeart, encrypt, keypair.getPublicKey()));
        }, Duration.ZERO, Duration.ofSeconds(2));
    }

    public FocessClientReceiver(final BothSideSocket socket, final String localhost, final String host, final int port, final String name) {
        this(socket, localhost, host, port, name, false, false);
    }

    @Override
    public void sendPacket(final Packet packet) {
        this.socket.sendClientPacket(this.host, this.port, new ClientPackPacket(this.id, this.token, packet));
    }

    @Override
    public void close() {
        this.scheduler.close();
        this.unregisterAll();
    }

    @Override
    public void disconnect() {
        this.socket.sendClientPacket(this.host, this.port, new DisconnectPacket(this.id, this.token));
        super.disconnect();
    }
}
