package top.focess.net;

/**
 * Represents a Client connected to a server.
 */
public interface Client {


    /**
     * Get the client name
     *
     * @return the client name
     */
    String getName();

    /**
     * Get the client id
     *
     * @return the client id
     */
    int getId();

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
}
