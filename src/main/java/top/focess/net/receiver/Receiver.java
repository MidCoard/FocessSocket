package top.focess.net.receiver;


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
}
