package top.focess.net;

import org.junit.jupiter.api.Test;
import top.focess.net.packet.MessagePacket;
import top.focess.net.socket.ASocket;
import top.focess.net.socket.FocessSidedClientSocket;

public class SocketTest {

    static {
        ASocket.enableDebug();
    }

    @Test
    public void testSocket() throws IllegalPortException, InterruptedException {
        FocessSidedClientSocket socket = new FocessSidedClientSocket("49.233.254.244",9321,"test");
        socket.getReceiver().register(MessagePacket.class, (id,packet) -> {
            System.out.println(packet.getMessage());
        });

        Thread.sleep(4000);
        socket.getReceiver().sendPacket(new MessagePacket("Hello"));
        socket.getReceiver().sendPacket(new MessagePacket("Goodbye"));
        socket.getReceiver().sendPacket(new MessagePacket("query"));
        Thread.sleep(5000);
    }
}
