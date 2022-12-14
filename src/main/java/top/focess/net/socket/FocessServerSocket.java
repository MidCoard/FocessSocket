package top.focess.net.socket;

import top.focess.net.IllegalPortException;
import top.focess.net.receiver.FocessReceiver;
import top.focess.net.receiver.ServerReceiver;

public class FocessServerSocket extends FocessSocket {

    public FocessServerSocket() throws IllegalPortException {
        super();
        this.registerReceiver(new FocessReceiver(this));
    }

    public FocessServerSocket(int port) throws IllegalPortException {
        super(port);
        this.registerReceiver(new FocessReceiver(this));
    }

    @Override
    public ServerReceiver getReceiver() {
        return (ServerReceiver) super.getReceiver();
    }
}
