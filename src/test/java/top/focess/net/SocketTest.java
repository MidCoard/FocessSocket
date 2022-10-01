package top.focess.net;

import org.junit.jupiter.api.Test;
import top.focess.net.receiver.FocessSidedClientReceiver;
import top.focess.net.receiver.FocessSidedReceiver;
import top.focess.net.socket.FocessSidedClientSocket;
import top.focess.net.socket.FocessSidedSocket;

public class SocketTest {

    @Test
    public void testSocket() throws IllegalPortException {
        FocessSidedSocket sidedSocket = new FocessSidedSocket(9081);
        sidedSocket.registerReceiver(new FocessSidedReceiver());
        FocessSidedClientSocket sidedClientSocket = new FocessSidedClientSocket("localhost",9081);
        sidedClientSocket.registerReceiver(new FocessSidedClientReceiver(sidedClientSocket,"fuck"));

    }
}
