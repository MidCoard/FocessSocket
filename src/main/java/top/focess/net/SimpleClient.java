package top.focess.net;

import org.jetbrains.annotations.Nullable;
import top.focess.util.RSA;
import top.focess.util.RSAKeypair;

public class SimpleClient implements Client {

    private final String host;
    private final int port;
    private final int id;
    private final String name;
    private final String token;

    private final boolean serverHeart;
    private final boolean encrypt;
    private final String key;

    private final RSAKeypair keypair;

    public SimpleClient(final String host, final int port, final int id, final String name, final String token, boolean serverHeart, boolean encrypt, String key) {
        this.host = host;
        this.port = port;
        this.id = id;
        this.name = name;
        this.token = token;
        this.serverHeart = serverHeart;
        this.encrypt = encrypt;
        this.key = key;
        if (encrypt)
            keypair = RSA.genRSAKeypair();
        else keypair = new RSAKeypair(null, null);
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

    public String getPublicKey() {
        return keypair.getPublicKey();
    }

    public String getPrivateKey() {
        return keypair.getPrivateKey();
    }
}
