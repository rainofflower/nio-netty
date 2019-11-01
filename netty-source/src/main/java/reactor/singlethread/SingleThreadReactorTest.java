package reactor.singlethread;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SingleThreadReactorTest {

    @Test
    public void startServer() throws IOException {
        new EchoServerReactor(8200).run();
    }

    /**
     * 客户端
     */
    @Test
    public void SocketChannel() throws IOException {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 8100));
        ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.put("hello".getBytes()).flip();
        socketChannel.write(buffer);

        // 读取响应
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        int num;
        if ((num = socketChannel.read(readBuffer)) > 0) {
            readBuffer.flip();

            byte[] re = new byte[num];
            readBuffer.get(re);

            String result = new String(re, "UTF-8");
            System.out.println("返回值: " + result);
        }
    }
}
