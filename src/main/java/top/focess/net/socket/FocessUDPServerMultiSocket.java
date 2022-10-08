package top.focess.net.socket;

import top.focess.net.IllegalPortException;
import top.focess.net.receiver.FocessUDPMultiReceiver;
import top.focess.net.receiver.ServerMultiReceiver;

public class FocessUDPServerMultiSocket extends FocessUDPSocket {
    public FocessUDPServerMultiSocket() throws IllegalPortException {
        super();
        this.registerReceiver(new FocessUDPMultiReceiver(this));
    }

    public FocessUDPServerMultiSocket(int port) throws IllegalPortException {
        super(port);
        this.registerReceiver(new FocessUDPMultiReceiver(this));
    }

    @Override
    public ServerMultiReceiver getReceiver() {
        return (ServerMultiReceiver) super.getReceiver();
    }
}
