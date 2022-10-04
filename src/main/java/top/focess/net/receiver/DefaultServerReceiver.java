package top.focess.net.receiver;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import top.focess.net.PacketHandler;
import top.focess.net.SimpleClient;
import top.focess.net.packet.DisconnectPacket;
import top.focess.net.packet.DisconnectedPacket;
import top.focess.net.packet.Packet;
import top.focess.net.socket.ASocket;
import top.focess.net.socket.Socket;

public abstract class DefaultServerReceiver extends AServerReceiver{

    public DefaultServerReceiver(Socket socket) {
        super(socket);
    }

    @PacketHandler
    public Packet onDisconnect(@NotNull final DisconnectPacket packet) {
        if (ASocket.isDebug())
            System.out.println("FocessSocket " + this + ": client " + packet.getClientId() + " disconnect");
        if (this.clientInfos.get(packet.getClientId()) != null) {
            final SimpleClient simpleClient = this.clientInfos.get(packet.getClientId());
            if (simpleClient.getToken().equals(packet.getToken())) {
                if (ASocket.isDebug())
                    System.out.println("FocessSocket " + this + ": server accept client " + packet.getClientId() + " disconnect");
                return this.disconnect(packet.getClientId());
            } else if (ASocket.isDebug())
                System.out.println("FocessSocket " + this + ": server reject client " + packet.getClientId() + " disconnect because of token conflict");
        } else if (ASocket.isDebug())
            System.out.println("FocessSocket " + this + ": server reject client " + packet.getClientId() + " disconnect because of client not exist");
        return null;
    }

    @NotNull
    @Contract("_ -> new")
    public DisconnectedPacket disconnect(final int clientId) {
        this.clientInfos.remove(clientId);
        return new DisconnectedPacket();
    }
}
