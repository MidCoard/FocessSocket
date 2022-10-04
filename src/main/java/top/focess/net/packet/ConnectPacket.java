package top.focess.net.packet;

import org.jetbrains.annotations.Nullable;

/**
 * Used to connect to the server.
 */
public class ConnectPacket extends Packet {

    public static final int PACKET_ID = 3;
    /**
     * The client host
     */
    private final String host;
    /**
     * The client port
     */
    private final int port;
    /**
     * The client name
     */
    private final String name;

    private final boolean serverHeart;
    private final boolean encrypt;
    @Nullable
    private final String key;

    /**
     * Constructs a ConnectPacket
     *
     * @param host the client host
     * @param port the client port
     * @param name the client name
     */
    public ConnectPacket(final String host, final int port, final String name) {
        this(host, port, name, false, false, null);
    }

    /**
     * Constructs a ConnectPacket
     *
     * @param host the client host
     * @param port the client port
     * @param name the client name
     * @param serverHeart whether the server need to send heart packet
     */
    public ConnectPacket(String host, int port, String name, boolean serverHeart) {
        this(host, port, name, serverHeart, false, null);
    }

    /**
     * Constructs a ConnectPacket
     *
     * @param host the client host
     * @param port the client port
     * @param name the client name
     * @param serverHeart whether the server need to send heart packet
     * @param encrypt whether the server/client need to encrypt the packet
     * @param key the key to encrypt the packet
     */
    public ConnectPacket(String host, int port, String name,boolean serverHeart, boolean encrypt,@Nullable String key) {
        this.host = host;
        this.port = port;
        this.name = name;
        this.encrypt = encrypt;
        this.serverHeart = serverHeart;
        this.key = key;
    }

    @Override
    public int getId() {
        return PACKET_ID;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getName() {
        return this.name;
    }

    public boolean isServerHeart() {
        return serverHeart;
    }

    public boolean isEncrypt() {
        return encrypt;
    }

    public @Nullable String getKey() {
        return key;
    }
}
