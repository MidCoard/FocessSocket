package top.focess.net.socket;

import top.focess.net.IllegalPortException;
import top.focess.net.receiver.FocessReceiver;
import top.focess.net.receiver.ServerReceiver;

public class FocessUDPServerSocket extends FocessUDPSocket {

    public FocessUDPServerSocket() throws IllegalPortException {
        super();
        this.registerReceiver(new FocessReceiver(this));
    }

    public FocessUDPServerSocket(int port) throws IllegalPortException {
        super(port);
        this.registerReceiver(new FocessReceiver(this));
    }

    @Override
    public ServerReceiver getReceiver() {
        return (ServerReceiver) super.getReceiver();
    }
}
