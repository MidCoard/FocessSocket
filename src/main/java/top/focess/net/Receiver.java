package top.focess.net;


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
