package top.focess.net.packet;

/**
 * Used to tell server/client the connection is not lost.
 */
public class HeartPacket extends Packet {

    public static final int PACKET_ID = 2;
    private final long time;

    private final int clientId;
    private final String token;

    /**
     * Constructs a HeartPacket
     *
     * @param clientId the client id
     * @param token    the client token
     * @param time     the client time
     */
    public HeartPacket(final int clientId, final String token, final long time) {
        this.clientId = clientId;
        this.token = token;
        this.time = time;
    }

    @Override
    public int getId() {
        return PACKET_ID;
    }

    public long getTime() {
        return this.time;
    }

    public int getClientId() {
        return clientId;
    }

    public String getToken() {
        return token;
    }
}
