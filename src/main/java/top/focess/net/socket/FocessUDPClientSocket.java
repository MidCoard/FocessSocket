package top.focess.net.socket;

import top.focess.net.IllegalPortException;
import top.focess.net.receiver.ClientReceiver;
import top.focess.net.receiver.FocessClientReceiver;

public class FocessUDPClientSocket extends FocessUDPSocket {
    public FocessUDPClientSocket(String localhost, String host, int port, String name, boolean serverHeart, boolean encrypt) throws IllegalPortException {
        super();
        this.registerReceiver(new FocessClientReceiver(this, localhost, host, port, name, serverHeart, encrypt));
    }

    public FocessUDPClientSocket(String localhost, int localPort, String host, int port, String name, boolean serverHeart, boolean encrypt) throws IllegalPortException {
        super(localPort);
        this.registerReceiver(new FocessClientReceiver(this, localhost, host, port, name, serverHeart, encrypt));
    }

    @Override
    public ClientReceiver getReceiver() {
        return (ClientReceiver) super.getReceiver();
    }
}
