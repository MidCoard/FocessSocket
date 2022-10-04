package top.focess.net.socket;

import top.focess.net.receiver.Receiver;

/**
 * Represents a FocessSocket. This class is used to handle socket.
 */
public interface Socket {

    /**
     * Register receiver for this socket
     *
     * @param receiver the receiver for this socket
     */
    void registerReceiver(Receiver receiver);

    /**
     * Indicate this socket contains server side receiver
     *
     * @return true if it contains server side receiver, false otherwise
     */
    boolean containsServerSide();

    /**
     * Indicate this socket contains client side receiver
     *
     * @return true if it contains client side receiver, false otherwise
     */
    boolean containsClientSide();

    /**
     * Close the socket, which will close all the receivers
     */
    void close();

    /**
     * Unregister all receivers, which will close all the receivers, but the socket will not be closed
     */
    void unregisterAll();

    /**
     * Unregister the receiver, which will close the receiver
     * @param receiver the receiver
     */
    void unregister(Receiver receiver);
}
