package top.focess.net;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.focess.net.packet.MessagePacket;
import top.focess.net.receiver.*;
import top.focess.net.socket.ASocket;
import top.focess.net.socket.FocessSocket;
import top.focess.net.socket.FocessUDPSocket;

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
        clientReceiver.waitConnected();
        clientReceiver.sendPacket(new MessagePacket("hello2"));
        Thread.sleep(2000);
        Assertions.assertEquals(atomicInteger.get(), 1);
        server.close();
        client.close();
    }

    @Test
    public void testUDPSocket() throws Exception {
        FocessUDPSocket focessUDPSocket = new FocessUDPSocket(1234);
        focessUDPSocket.registerReceiver(new FocessUDPReceiver(focessUDPSocket));
        AtomicInteger atomicInteger = new AtomicInteger(0);
        ServerReceiver serverReceiver = (ServerReceiver) focessUDPSocket.getReceiver();
        serverReceiver.register("hello",MessagePacket.class,(clientId, packet) -> {
            System.out.println("Server received hello from " + clientId);
            System.out.println("client send: " + packet.getMessage());
            Assertions.assertEquals("hello", packet.getMessage());
            atomicInteger.incrementAndGet();
        });
        FocessUDPSocket focessUDPClientSocket = new FocessUDPSocket(1321);
        focessUDPClientSocket.registerReceiver(new FocessUDPClientReceiver(focessUDPClientSocket,"localhost","localhost",1234, "hello" ));
        ClientReceiver clientReceiver = (ClientReceiver) focessUDPClientSocket.getReceiver();
        clientReceiver.waitConnected();
        clientReceiver.sendPacket(new MessagePacket("hello"));
        Thread.sleep(2000);
        Assertions.assertEquals(atomicInteger.get(), 1);
        focessUDPSocket.close();
        focessUDPClientSocket.close();
    }
}
