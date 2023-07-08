package reactor.multithread2;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class MultiThreadReactorTest {

    /**
     * 启动服务端
     */
    @Test
    public void startServer() throws IOException {
        Thread.currentThread().setName("主线程");
        MultiThreadEchoServerReactor multiThreadEchoServerReactor = new MultiThreadEchoServerReactor(8200,11);
        multiThreadEchoServerReactor.startService();
    }

    /**
     * 启动客户端
     */
    public static void main(String... args){
        try {
            startClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startClient() throws IOException {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 8200));
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        Thread readThread = new Thread(new ChannelInboundHandler(socketChannel));
        readThread.setDaemon(true);
        readThread.start();
        Scanner scanner = new Scanner(System.in);
        System.out.println("输入消息内容：");
        while(true){
            try{
                String input = scanner.nextLine();
                if(input != null && input.equals("exit")){
                    return;
                }
                else{
                    buffer.put(input.getBytes()).flip();
                    socketChannel.write(buffer);
//                    socketChannel.shutdownOutput();
                    buffer.clear();
                }
            }catch (IOException e){
                e.printStackTrace();
                if(socketChannel != null){
                    socketChannel.close();
                }
                return;
            }
        }
    }

    static class ChannelInboundHandler implements Runnable{

        final SocketChannel socketChannel;

        ChannelInboundHandler(SocketChannel socketChannel){
            this.socketChannel = socketChannel;
        }

        public void run() {
            while(true){
                try{
                    // 读取响应
                    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                    int num;
                    if ((num = socketChannel.read(readBuffer)) > 0) {
                        readBuffer.flip();

                        byte[] re = new byte[num];
                        readBuffer.get(re);

                        String result = new String(re, "UTF-8");
                        System.out.println("服务端响应: " + result);
                    }
                }catch (IOException e){
                    e.printStackTrace();
                    if(socketChannel != null){
                        try {
                            socketChannel.close();
                            return;
                        } catch (IOException e1) {
                            //
                        }
                    }
                }
            }
        }
    }
}
