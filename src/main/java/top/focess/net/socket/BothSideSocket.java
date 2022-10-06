package top.focess.net.socket;

import top.focess.net.SimpleClient;
import top.focess.net.packet.*;
import top.focess.net.receiver.Receiver;
import top.focess.net.receiver.ServerReceiver;
import top.focess.scheduler.FocessScheduler;
import top.focess.scheduler.Scheduler;

import java.time.Duration;

public abstract class BothSideSocket extends ASocket{

    protected int localPort;

    public abstract boolean sendClientPacket(final String host, final int port, final ClientPacket packet);

    public abstract boolean sendServerPacket(SimpleClient client, String host, int port, ServerPacket serverPacket);

    public boolean sendServerPacket(SimpleClient client, ServerPacket serverPacket){
        if (client == null)
            return false;
        return this.sendServerPacket(client,client.getHost(),client.getPort(),serverPacket);
    }

    @Override
    public void registerReceiver(Receiver receiver) {
        if (this.isClientSide() && receiver.isServerSide())
            throw new UnsupportedOperationException();
        if (this.isServerSide() && receiver.isClientSide())
            throw new UnsupportedOperationException();
        super.registerReceiver(receiver);
    }

    public int getLocalPort() {
        return this.localPort;
    }
}
