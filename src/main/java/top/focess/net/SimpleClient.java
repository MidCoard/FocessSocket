package top.focess.net;

import org.jetbrains.annotations.Nullable;

public class SimpleClient implements Client {

    private final String host;
    private final int port;
    private final int id;
    private final String name;
    private final String token;

    private final boolean serverHeart;
    private final boolean encrypt;
    private final String key;

    public SimpleClient(final String host, final int port, final int id, final String name, final String token) {
        this(host, port, id, name, token, false, false, null);
    }

    public SimpleClient(final String host, final int port, final int id, final String name, final String token, boolean serverHeart, boolean encrypt, String key) {
        this.host = host;
        this.port = port;
        this.id = id;
        this.name = name;
        this.token = token;
        this.serverHeart = serverHeart;
        this.encrypt = encrypt;
        this.key = key;
    }

    public SimpleClient(final int id, final String name, final String token) {
        this(null, -1, id, name, token);
    }

    public SimpleClient(final int id, final String name, final String token, boolean serverHeart, boolean encrypt, String key) {
        this(null, -1, id, name, token, serverHeart, encrypt, key);
    }

    @Nullable
    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public int getId() {
        return this.id;
    }

    public String getToken() {
        return this.token;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean isServerHeart() {
        return serverHeart;
    }

    @Override
    public boolean isEncrypt() {
        return encrypt;
    }

    public String getKey() {
        return key;
    }
}
