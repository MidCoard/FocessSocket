package top.focess.net.receiver;


import top.focess.net.DisconnectedHandler;
import top.focess.net.PackHandler;

/**
 * The class is used to handle packet.
 */
public interface Receiver {
    /**
     * Close the receiver.
     */
    void close();

    /**
     * Unregister all the packet handlers
     */
    void unregisterAll();

    /**
     * Unregister the packet handler
     *
     * @param handler the handler to packet unregister
     */
    void unregister(PackHandler handler);

    /**
     * Indicate this receiver is client side
     *
     * @return true if it is client side, false otherwise
     */
    boolean isClientSide();

    /**
     * Indicate this receiver is server side
     *
     * @return true if it is server side, false otherwise
     */
    boolean isServerSide();

    void setDisconnectedHandler(DisconnectedHandler disconnectedHandler);
}
