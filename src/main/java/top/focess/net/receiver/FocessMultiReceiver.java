package top.focess.net.receiver;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import top.focess.net.Client;
import top.focess.net.PacketHandler;
import top.focess.net.SimpleClient;
import top.focess.net.packet.ConnectPacket;
import top.focess.net.packet.ConnectedPacket;
import top.focess.net.packet.Packet;
import top.focess.net.packet.ServerPackPacket;
import top.focess.net.socket.ASocket;
import top.focess.net.socket.BothSideSocket;

import java.util.Collections;
import java.util.List;

public class FocessMultiReceiver extends FocessReceiver implements ServerMultiReceiver{
    public FocessMultiReceiver(BothSideSocket socket) {
        super(socket);
    }

    @PacketHandler
    public synchronized void onConnect(@NotNull final ConnectPacket packet) {
        if (ASocket.isDebug())
            System.out.println("SM FocessSocket: server accept client " + packet.getName() + " connecting from " + packet.getHost() + ":" + packet.getPort());
        final SimpleClient simpleClient = new SimpleClient(packet.getHost(), packet.getPort(), this.defaultClientId.incrementAndGet(), packet.getName(), generateToken(), packet.isServerHeart(), packet.isEncrypt(), packet.getKey());
        this.lastHeart.put(simpleClient.getId(), System.currentTimeMillis());
        this.clientInfos.put(simpleClient.getId(), simpleClient);
        ((BothSideSocket) this.socket).sendServerPacket(simpleClient, new ConnectedPacket(simpleClient.getId(), simpleClient.getToken(), simpleClient.getPublicKey()));
    }

    @Override
    public @UnmodifiableView List<Client> getClients(final String name) {
        final List<Client> ret = Lists.newArrayList();
        for (final SimpleClient client : this.clientInfos.values())
            if (client.getName().equals(name))
                ret.add(client);
        return Collections.unmodifiableList(ret);
    }

    @Override
    public void sendPacket(String client, Packet packet) {
        this.getClients(client).forEach(
                c -> ((BothSideSocket) this.socket).sendServerPacket((SimpleClient) c, new ServerPackPacket(packet))
        );
    }
}
