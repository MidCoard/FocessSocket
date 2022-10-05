package top.focess.net;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.focess.net.packet.MessagePacket;
import top.focess.net.receiver.FocessClientReceiver;
import top.focess.net.receiver.FocessReceiver;
import top.focess.net.receiver.ServerReceiver;
import top.focess.net.socket.ASocket;
import top.focess.net.socket.FocessSocket;

import java.util.concurrent.atomic.AtomicInteger;

public class NormalTest {

    static {
        ASocket.enableDebug();
    }

    @Test
    public void testSocket() throws IllegalPortException, InterruptedException {
        FocessSocket server = new FocessSocket(1234);
        server.registerReceiver(new FocessReceiver(server));
        ServerReceiver serverReceiver = (ServerReceiver) server.getReceiver();
        AtomicInteger atomicInteger = new AtomicInteger(0);
        serverReceiver.register("hello", MessagePacket.class, (clientId, packet) -> {
            System.out.println("Server received hello from " + clientId);
            System.out.println("client send: " + packet.getMessage());
            atomicInteger.incrementAndGet();
            Assertions.assertEquals("hello2", packet.getMessage());
        });
        FocessSocket client = new FocessSocket(1321);
        client.registerReceiver(new FocessClientReceiver(client, "localhost", "localhost", 1234, "hello"));
        FocessClientReceiver clientReceiver = (FocessClientReceiver) client.getReceiver();
        clientReceiver.sendPacket(new MessagePacket("hello2"));
        Assertions.assertEquals(atomicInteger.get(), 1);
    }
}
