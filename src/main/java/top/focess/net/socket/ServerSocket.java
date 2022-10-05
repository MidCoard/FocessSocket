package top.focess.net.socket;

import top.focess.net.receiver.ServerReceiver;

public class ServerSocket extends ASocket {


    @Override
    public ServerReceiver getReceiver() {
        return (ServerReceiver) super.getReceiver();
    }
}
