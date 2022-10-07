package top.focess.net.packet;

/**
 * Used to tell client the id and the token
 */
public class ConnectedPacket extends ServerPacket {

    public static final int PACKET_ID = 4;
    /**
     * The client id
     */
    private final int clientId;
    /**
     * The token
     */
    private final String token;

    /**
     * The public key
     */
    private final String key;

    /**
     * Constructs a ConnectedPacket
     *
     * @param clientId the client id
     * @param token    the token
     * @param key      the  public key
     */
    public ConnectedPacket(final int clientId, final String token, final String key) {
        this.clientId = clientId;
        this.token = token;
        this.key = key;
    }

    @Override
    public int getId() {
        return PACKET_ID;
    }

    public int getClientId() {
        return this.clientId;
    }

    public String getToken() {
        return this.token;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "ConnectedPacket{" +
                "clientId=" + clientId +
                ", token='" + token + '\'' +
                ", key='" + key + '\'' +
                "} " + super.toString();
    }
}
