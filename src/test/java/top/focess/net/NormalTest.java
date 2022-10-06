package top.focess.net;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.focess.net.packet.MessagePacket;
import top.focess.net.receiver.*;
import top.focess.net.socket.*;

import java.util.concurrent.TimeUnit;
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
        focessUDPSocket.registerReceiver(new FocessReceiver(focessUDPSocket));
        AtomicInteger atomicInteger = new AtomicInteger(0);
        ServerReceiver serverReceiver = (ServerReceiver) focessUDPSocket.getReceiver();
        serverReceiver.register("hello",MessagePacket.class,(clientId, packet) -> {
            System.out.println("Server received hello from " + clientId);
            System.out.println("client send: " + packet.getMessage());
            Assertions.assertEquals("hello", packet.getMessage());
            atomicInteger.incrementAndGet();
        });
        FocessUDPSocket focessUDPClientSocket = new FocessUDPSocket(1321);
        focessUDPClientSocket.registerReceiver(new FocessClientReceiver(focessUDPClientSocket,"localhost","localhost",1234, "hello" ));
        ClientReceiver clientReceiver = (ClientReceiver) focessUDPClientSocket.getReceiver();
        clientReceiver.waitConnected();
        clientReceiver.sendPacket(new MessagePacket("hello"));
        Thread.sleep(2000);
        Assertions.assertEquals(atomicInteger.get(), 1);
        FocessUDPSocket focessUDPSocket1 = new FocessUDPSocket(2222);
        focessUDPSocket1.registerReceiver(new FocessClientReceiver(focessUDPSocket1,"localhost","localhost",1234, "hello" ));
        Assertions.assertFalse(((ClientReceiver) focessUDPSocket1.getReceiver()).waitConnected(5, TimeUnit.SECONDS));
        focessUDPSocket1.close();
        focessUDPSocket.close();
        focessUDPClientSocket.close();
    }

    @Test
    public void testUDPMultiSocket() throws Exception {
        FocessUDPSocket focessUDPSocket = new FocessUDPSocket(1234);
        focessUDPSocket.registerReceiver(new FocessUDPMultiReceiver(focessUDPSocket));
        AtomicInteger atomicInteger = new AtomicInteger(0);
        ServerReceiver serverReceiver = (ServerReceiver) focessUDPSocket.getReceiver();
        serverReceiver.register("hello",MessagePacket.class,(clientId, packet) -> {
            System.out.println("Server received hello from " + clientId);
            System.out.println("client send: " + packet.getMessage());
            Assertions.assertEquals("hello", packet.getMessage());
            atomicInteger.incrementAndGet();
        });
        FocessUDPSocket focessUDPClientSocket = new FocessUDPSocket(1321);
        focessUDPClientSocket.registerReceiver(new FocessClientReceiver(focessUDPClientSocket,"localhost","localhost",1234, "hello" ));
        ClientReceiver clientReceiver = (ClientReceiver) focessUDPClientSocket.getReceiver();
        clientReceiver.waitConnected();
        clientReceiver.sendPacket(new MessagePacket("hello"));
        Thread.sleep(2000);
        Assertions.assertEquals(atomicInteger.get(), 1);
        FocessUDPSocket focessUDPSocket1 = new FocessUDPSocket(2222);
        focessUDPSocket1.registerReceiver(new FocessClientReceiver(focessUDPSocket1,"localhost","localhost",1234, "hello" ));
        Assertions.assertTrue(((ClientReceiver) focessUDPSocket1.getReceiver()).waitConnected(5, TimeUnit.SECONDS));
        focessUDPSocket1.close();
        focessUDPSocket.close();
        focessUDPClientSocket.close();
    }

    @Test
    public void testSidedSocket() throws Exception {
        FocessSidedSocket server = new FocessSidedSocket(1234);
        AtomicInteger atomicInteger = new AtomicInteger(0);
        server.getReceiver().register("focess", MessagePacket.class, (clientId, packet) -> {
            System.out.println("Server received focess from " + clientId);
            System.out.println("client send: " + packet.getMessage());
            Assertions.assertEquals("hello", packet.getMessage());
            atomicInteger.incrementAndGet();
        });
        FocessSidedClientSocket client = new FocessSidedClientSocket("localhost", 1234, "focess");
        client.getReceiver().waitConnected();
        client.getReceiver().sendPacket(new MessagePacket("hello"));
        Thread.sleep(2000);
        Assertions.assertEquals(atomicInteger.get(), 1);
        FocessSidedClientSocket client1 = new FocessSidedClientSocket("localhost", 1234, "focess");
        Assertions.assertFalse(client1.getReceiver().waitConnected(5, TimeUnit.SECONDS));
        client1.close();
        client.close();
        server.close();
    }
}
