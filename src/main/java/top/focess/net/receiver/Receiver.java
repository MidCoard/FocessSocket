package top.focess.net.receiver;


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
     * @param handler the handler to packet unregister
     */
    void unregister(PackHandler handler);
}
