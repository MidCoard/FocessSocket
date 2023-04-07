package top.focess.net.socket;

import top.focess.net.IllegalPortException;
import top.focess.net.receiver.FocessMultiReceiver;
import top.focess.net.receiver.ServerMultiReceiver;

public class FocessUDPMultiSocket extends FocessUDPSocket {
    public FocessUDPMultiSocket() throws IllegalPortException {
        super();
        this.registerReceiver(new FocessMultiReceiver(this));
    }

    public FocessUDPMultiSocket(int port) throws IllegalPortException {
        super(port);
        this.registerReceiver(new FocessMultiReceiver(this));
    }

    @Override
    public ServerMultiReceiver getReceiver() {
        return (ServerMultiReceiver) super.getReceiver();
    }
}
