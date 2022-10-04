package top.focess.net.receiver;

import top.focess.net.PackHandler;
import top.focess.net.packet.Packet;

/**
 * The socket receiver for client.
 */
public interface ClientReceiver extends Receiver {

    /**
     * Send the packet to the server
     *
     * @param packet the packet
     */
    void sendPacket(Packet packet);

    /**
     * Register packet handler for server
     *
     * @param c           the packet class
     * @param packHandler the packet handler
     * @param <T>         the packet type
     */
    <T extends Packet> void register(Class<T> c, PackHandler<T> packHandler);


    /**
     * Get the name of the client
     *
     * @return the name of the client
     */
    String getName();

    /**
     * Get the target host of the client
     *
     * @return the target host of the client
     */
    String getHost();

    /**
     * Get the target port of the client
     *
     * @return the target port of the client
     */
    int getPort();

    /**
     * Indicate this client has connected to a server
     *
     * @return true if the client has connected to a server, false otherwise
     */
    boolean isConnected();

    /**
     * Get the client id
     *
     * @return the client id
     */
    int getClientId();

    /**
     * Get the client token
     *
     * @return the client token
     */
    String getClientToken();

    @Override
    default boolean isClientSide() {
        return true;
    }

    @Override
    default boolean isServerSide() {
        return false;
    }

    /**
     * Indicate whether the server need to send heart packet
     * @return true if the server need to send heart packet, false otherwise
     */
    boolean isServerHeart();

    /**
     * Indicate whether the server/client need to encrypt the packet
     * @return true if the server/client need to encrypt the packet, false otherwise
     */
    boolean isEncrypt();

    /**
     * Get the private key to decrypt the packet
     * @return the private key to decrypt the packet
     */
    String getPrivateKey();

    /**
     * Get the public key to encrypt the packet
     * @return the public key to encrypt the packet
     */
    String getKey();

    /**
     * Disconnect the client
     */
    void disconnect();
}
