package top.focess.net.socket;

import top.focess.net.IllegalPortException;
import top.focess.net.receiver.FocessReceiver;

public class FocessServerSocket extends FocessSocket {

    public FocessServerSocket() throws IllegalPortException {
        super();
        this.registerReceiver(new FocessReceiver(this));
    }

    public FocessServerSocket(int port) throws IllegalPortException {
        super(port);
        this.registerReceiver(new FocessReceiver(this));
    }
}
