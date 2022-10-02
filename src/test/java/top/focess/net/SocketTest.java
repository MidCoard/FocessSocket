package top.focess.net;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import top.focess.net.packet.MessagePacket;
import top.focess.net.receiver.FocessClientReceiver;
import top.focess.net.receiver.ServerReceiver;
import top.focess.net.socket.ASocket;
import top.focess.net.socket.FocessSidedClientSocket;
import top.focess.net.socket.FocessSidedSocket;
import top.focess.net.socket.FocessSocket;

import java.util.concurrent.atomic.AtomicReference;

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
