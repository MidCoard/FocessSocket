package top.focess.net.packet;

/**
 * The class indicates that this packet is from client side to server side.
 */
public abstract class ClientPacket extends Packet {

    /**
     * The client token
     */
    private final String token;
    /**
     * The client id
     */
    private int clientId;

    /**
     * Constructs a ClientPacket
     *
     * @param clientId the client id
     * @param token    the client token
     */
    public ClientPacket(final int clientId, final String token) {
        this.clientId = clientId;
        this.token = token;
    }

    public int getClientId() {
        return this.clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getToken() {
        return this.token;
    }
}
