package top.focess.net.socket;

import top.focess.net.IllegalPortException;
import top.focess.net.receiver.FocessClientReceiver;

public class FocessClientSocket extends FocessSocket {

    public FocessClientSocket(String localhost, String host, int port, String name, boolean serverHeart, boolean encrypt) throws IllegalPortException {
        super();
        this.registerReceiver(new FocessClientReceiver(this, localhost, host, port, name, serverHeart, encrypt));
    }

    public FocessClientSocket(String localhost, int localPort, String host, int port, String name, boolean serverHeart, boolean encrypt) throws IllegalPortException {
        super(localPort);
        this.registerReceiver(new FocessClientReceiver(this, localhost, host, port, name, serverHeart, encrypt));
    }
}
