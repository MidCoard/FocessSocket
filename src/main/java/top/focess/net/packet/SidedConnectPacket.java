package top.focess.net.packet;

/**
 * Used to connect to the server.
 */
public class SidedConnectPacket extends ClientPacket {

    public static final int PACKET_ID = 9;
    /**
     * The client name
     */
    private final String name;
    private final boolean serverHeart;
    private final boolean encrypt;
    private final String key;

    /**
     * Constructs a SidedConnectPacket
     *
     * @param name the client name
     */
    public SidedConnectPacket(final String name, final boolean serverHeart, final boolean encrypt, final String key) {
        super(-1, null);
        this.name = name;
        this.serverHeart = serverHeart;
        this.encrypt = encrypt;
        this.key = key;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public int getId() {
        return PACKET_ID;
    }

    public boolean isServerHeart() {
        return serverHeart;
    }

    public boolean isEncrypt() {
        return encrypt;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "SidedConnectPacket{" +
                "name='" + name + '\'' +
                ", serverHeart=" + serverHeart +
                ", encrypt=" + encrypt +
                ", key='" + key + '\'' +
                "} " + super.toString();
    }
}
