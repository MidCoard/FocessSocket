package top.focess.net;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import top.focess.net.packet.MessagePacket;
import top.focess.net.receiver.ServerReceiver;
import top.focess.net.socket.FocessSidedClientSocket;
import top.focess.net.socket.FocessSidedSocket;

import java.util.concurrent.atomic.AtomicReference;

public class SocketTest {

    @RepeatedTest(5)
    public void testSocket() throws IllegalPortException, InterruptedException {
        FocessSidedSocket sidedSocket = new FocessSidedSocket(9081);
        FocessSidedClientSocket sidedClientSocket = new FocessSidedClientSocket("localhost", 9081, "fuck");
        ServerReceiver serverReceiver = sidedSocket.getReceiver();
        AtomicReference<String> reference = new AtomicReference<>();
        serverReceiver.register("fuck", MessagePacket.class, (packet) -> {
            Assertions.assertEquals(packet.getMessage(), "fuckyu");
            reference.set("fuckyu");
        });

        Thread.sleep(1000);

        sidedClientSocket.getReceiver().sendPacket(new MessagePacket("fuckyu"));
        Thread.sleep(2000);
        Assertions.assertEquals(reference.get(), "fuckyu");
        sidedSocket.close();
        sidedClientSocket.close();
    }
}
