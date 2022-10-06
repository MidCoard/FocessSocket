package top.focess.net.packet;

/**
 * Used to tell client the connection is not lost.
 */
public class ServerHeartPacket extends ServerPacket{


    public static final int PACKET_ID = 11;
    private final long time;

    /**
     * Constructs a ServerHeartPacket
     * @param time the server time
     */
    public ServerHeartPacket(long time) {
        this.time = time;
    }

    @Override
    public int getId() {
        return PACKET_ID;
    }


    public long getTime() {
        return time;
    }
}
