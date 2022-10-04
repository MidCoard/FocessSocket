package top.focess.net.receiver;

import org.jetbrains.annotations.Nullable;
import top.focess.net.Client;
import top.focess.net.PackHandler;
import top.focess.net.packet.DisconnectedPacket;
import top.focess.net.packet.Packet;

/**
 * The socket receiver for server.
 */
public interface ServerReceiver extends Receiver {

    /**
     * Send packet to the special client
     *
     * @param client the client name
     * @param packet the packet
     */
    void sendPacket(String client, Packet packet);

    /**
     * Register packet handler for special client
     *
     * @param client      the client name
     * @param c           the packet class
     * @param packHandler the packet handler
     * @param <T>         the packet type
     */
    <T extends Packet> void register(String client, Class<T> c, PackHandler<T> packHandler);


    /**
     * Indicate the client is connected to server
     *
     * @param client the client name
     * @return true if the client is connected to server, false otherwise
     */
    boolean isConnected(String client);

    /**
     * Disconnect the client
     *
     * @param client the client name
     */
    void disconnect(String client);

    /**
     * Get the client by given name
     *
     * @param name the client name
     * @return the client
     */
    @Nullable
    Client getClient(String name);


    /**
     * Disconnect the client by given id
     *
     * @param id the client id
     * @return the disconnected packet
     */
    DisconnectedPacket disconnect(int id);

    @Override
    default boolean isServerSide() {
        return true;
    }

    @Override
    default boolean isClientSide() {
        return false;
    }
}
