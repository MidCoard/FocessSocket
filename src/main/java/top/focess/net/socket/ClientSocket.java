package top.focess.net.socket;

import top.focess.net.receiver.ClientReceiver;

public class ClientSocket extends ASocket {

    protected final String host;
    protected final int port;

    public ClientSocket(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    @Override
    public ClientReceiver getReceiver() {
        return (ClientReceiver) super.getReceiver();
    }
}
