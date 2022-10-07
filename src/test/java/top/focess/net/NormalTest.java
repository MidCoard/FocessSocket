package top.focess.net;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.focess.net.packet.MessagePacket;
import top.focess.net.receiver.*;
import top.focess.net.socket.*;
import top.focess.util.RSA;
import top.focess.util.RSAKeypair;

import java.nio.charset.StandardCharsets;
import java.util.Random;
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

    @Test
    public void testReconnect() throws Exception {
        FocessSocket socket = new FocessSocket(1234);
        socket.registerReceiver(new FocessReceiver(socket));
        FocessSocket client = new FocessSocket();
        client.registerReceiver(new FocessClientReceiver(client, "localhost", "localhost", 1234, "hello"));
        ClientReceiver clientReceiver = (ClientReceiver) client.getReceiver();
        clientReceiver.waitConnected();
        clientReceiver.disconnect();
        Assertions.assertTrue(clientReceiver.waitConnected(5, TimeUnit.SECONDS));
        socket.close();
        client.close();
    }

    @Test
    public void testClientReconnect0() throws Exception {
        FocessSocket socket = new FocessSocket(1234);
        socket.registerReceiver(new FocessReceiver(socket));
        FocessSocket client = new FocessSocket();
        client.registerReceiver(new FocessClientReceiver(client, "localhost", "localhost", 1234, "hello"));
        ClientReceiver clientReceiver = (ClientReceiver) client.getReceiver();
        Assertions.assertTrue(clientReceiver.waitConnected(5, TimeUnit.SECONDS));
        Assertions.assertNotNull(((ServerReceiver) socket.getReceiver()).getClient("hello"));
        socket.close();
        FocessSocket socket1 = new FocessSocket(1234);
        socket1.registerReceiver(new FocessReceiver(socket1));
        Thread.sleep(15000);
        Assertions.assertNull(((ServerReceiver) socket1.getReceiver()).getClient("hello"));
        socket1.close();
        client.close();
    }

    @Test
    public void testClientReconnect1() throws Exception {
        FocessSocket socket = new FocessSocket(1234);
        socket.registerReceiver(new FocessReceiver(socket));
        FocessSocket client = new FocessSocket();
        client.registerReceiver(new FocessClientReceiver(client, "localhost", "localhost", 1234, "hello", true, false));
        ClientReceiver clientReceiver = (ClientReceiver) client.getReceiver();
        Assertions.assertTrue(clientReceiver.waitConnected(5, TimeUnit.SECONDS));
        Assertions.assertNotNull(((ServerReceiver) socket.getReceiver()).getClient("hello"));
        socket.close();
        FocessSocket socket1 = new FocessSocket(1234);
        socket1.registerReceiver(new FocessReceiver(socket1));
        Thread.sleep(15000);
        Assertions.assertNotNull(((ServerReceiver) socket1.getReceiver()).getClient("hello"));
        socket1.close();
        client.close();
    }

    @Test
    public void testClientEncrypt() throws Exception {
        FocessSidedSocket socket = new FocessSidedSocket(1234);
        AtomicInteger atomicInteger = new AtomicInteger(0);
        socket.getReceiver().register("focess", MessagePacket.class, (clientId, packet) -> {
            System.out.println("Server received focess from " + clientId);
            System.out.println("client send: " + packet.getMessage());
            Assertions.assertEquals("hello", packet.getMessage());
            atomicInteger.incrementAndGet();
        });
        FocessSidedClientSocket client = new FocessSidedClientSocket("localhost", 1234, "focess", false, true);
        client.getReceiver().waitConnected();
        client.getReceiver().sendPacket(new MessagePacket("hello"));
        Thread.sleep(2000);
        Assertions.assertEquals(atomicInteger.get(), 1);
        socket.close();
        FocessSidedSocket socket1 = new FocessSidedSocket(1234);
        Thread.sleep(15000);
        Assertions.assertNull(socket1.getReceiver().getClient("focess"));
        socket1.close();
        client.close();
    }

    @Test
    public void testRSA() {
        RSAKeypair keyPair = RSA.genRSAKeypair();
        byte[] bytes = new byte[1024];
        new Random().nextBytes(bytes);
        byte[] encrypt = RSA.encryptRSA(bytes, keyPair.getPublicKey());
        byte[] decrypt = RSA.decryptRSA(encrypt, keyPair.getPrivateKey());
        Assertions.assertArrayEquals(bytes, decrypt);
    }

    @Test
    public void testRSA2() {
        byte[] bytes = new byte[]{4, 0, 0, 0, 1, 0, 0, 0, 64, 0, 0, 0, 90, 109, 97, 68, 113, 120, 77, 108, 122, 108, 48, 108, 73, 66, 117, 105, 67, 105, 108, 65, 105, 112, 113, 107, 114, 120, 55, 51, 79, 49, 102, 52, 101, 105, 113, 55, 49, 75, 112, 53, 82, 107, 52, 121, 97, 72, 51, 89, 85, 49, 89, 107, 55, 57, 49, 113, 114, 83, 72, 81, 50, 48, 66, 113, -40, 0, 0, 0, 77, 73, 71, 102, 77, 65, 48, 71, 67, 83, 113, 71, 83, 73, 98, 51, 68, 81, 69, 66, 65, 81, 85, 65, 65, 52, 71, 78, 65, 68, 67, 66, 105, 81, 75, 66, 103, 81, 67, 121, 73, 108, 112, 99, 78, 49, 66, 72, 98, 81, 55, 72, 86, 117, 84, 48, 121, 75, 53, 70, 121, 108, 69, 65, 101, 100, 73, 47, 52, 118, 52, 90, 78, 50, 69, 118, 72, 66, 101, 103, 48, 73, 77, 119, 121, 79, 72, 101, 110, 116, 65, 79, 99, 86, 88, 121, 75, 65, 49, 115, 78, 52, 80, 72, 51, 54, 119, 78, 80, 112, 119, 104, 100, 85, 112, 67, 43, 53, 77, 105, 110, 77, 79, 84, 111, 73, 109, 53, 79, 89, 111, 80, 72, 88, 79, 68, 110, 73, 77, 112, 80, 66, 67, 74, 80, 109, 105, 99, 89, 115, 97, 80, 79, 112, 54, 88, 88, 106, 115, 65, 72, 121, 85, 86, 88, 110, 106, 108, 120, 89, 74, 84, 85, 48, 48, 90, 112, 99, 113, 80, 119, 51, 119, 50, 52, 110, 100, 118, 101, 112, 85, 110, 75, 84, 81, 73, 87, 43, 118, 53, 105, 98, 120, 122, 119, 65, 115, 112, 99, 119, 73, 68, 65, 81, 65, 66};
        RSAKeypair keyPair = RSA.genRSAKeypair();
        System.out.println(keyPair.getPublicKey());
        System.out.println(keyPair.getPrivateKey());
        byte[] encrypt = RSA.encryptRSA(bytes, keyPair.getPublicKey());
        byte[] decrypt = RSA.decryptRSA(encrypt, keyPair.getPrivateKey());
        Assertions.assertArrayEquals(bytes, decrypt);
    }

}
