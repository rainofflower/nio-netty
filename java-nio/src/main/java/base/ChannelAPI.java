package base;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import util.CompositeThreadPoolConfig;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Channel 经常翻译为通道，类似 IO 中的流，用于读取和写入。
 * 读操作的时候将 Channel 中的数据填充到 Buffer 中，
 * 而写操作时将 Buffer 中的数据写入到 Channel 中
 *
 * 实现类：
 * FileChannel：文件通道，用于文件的读和写
 * DatagramChannel：用于 UDP 连接的接收和发送
 * SocketChannel：把它理解为 TCP 连接通道，可读可写，对于一个 TCP 连接，客户端服务端各有一个SocketChannel对应
 * ServerSocketChannel：TCP 对应的服务端，用于监听某个端口进来的请求。
 *
 * 注意：FileChannel 不支持非阻塞模式，文件 IO 在所有的操作系统中都不支持非阻塞模式
 */
@Slf4j
public class ChannelAPI {

    /**
     *
     * 阻塞IO
     *
     *
     * ServerSocketChannel 不和 Buffer 打交道，
     * 因为它并不实际处理数据，它一旦接收到请求后，实例化 SocketChannel，之后在这个连接通道上的数据传递它就不管了，
     * 因为它需要继续监听端口，等待下一个连接
     *
     * Blocking IO模式性能瓶颈：
     * socketChannel.read()和socketChannel.write()会阻塞当前线程，而每个线程都需要一部分内存，不工作线程会白白浪费内存，
     * 同时，线程量大了之后线程切换的开销非常大。
     * --> accept() 是一个阻塞操作，当 accept() 返回的时候，代表有一个连接可以使用了，我们这里是马上就提交线程池来处理这个 SocketChannel 了，
     * 但是，这里不代表对方就将数据传输过来了。所以，SocketChannel#read 方法将阻塞，等待数据，明显这个等待是不值得的。
     * 同理，write 方法也需要等待通道可写才能执行写入操作，这边的阻塞等待也是不值得的
     *
     * 以下为 Blocking IO 示例
     */
    @Test
    public void ServerSocketChannel() throws IOException {
        ThreadPoolExecutor threadPoolExecutor = new CompositeThreadPoolConfig().threadPoolExecutor();
        //实例化
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //监听8080端口
        serverSocketChannel.socket().bind(new InetSocketAddress(8080));
        while(true){
            // 一旦有一个 TCP 连接进来，就对应创建一个 SocketChannel 进行处理
            SocketChannel socketChannel = serverSocketChannel.accept();
            threadPoolExecutor.execute(()->{
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                try{
                    int num;
                    //将数据读入buffer
                    while((num = socketChannel.read(buffer))>0){
                        //读取之前调用flip
                        buffer.flip();
                        byte[] bytes = new byte[num];
                        //提取buffer的数据
                        buffer.get(bytes);
                        String re = new String(bytes, "UTF-8");
                        log.info("收到请求："+re);

                        //响应请求
                        ByteBuffer response = ByteBuffer.wrap(("我已经收到你的请求，你的请求内容是：" + re).getBytes());
                        socketChannel.write(response);
                        buffer.clear();
                    }
                }catch (IOException e){
                    if(socketChannel != null){
                        try {
                            socketChannel.close();
                        } catch (IOException e1) {
                            //
                        }
                    }
                }
            });
        }
    }

    /**
     * 客户端
     */
    @Test
    public void SocketChannel() throws IOException {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("localhost", 8080));
        /*
         *  上方打开TCP连接的方式等价于以下两行
         *  //打开一个通道
         *  SocketChannel socketChannel = SocketChannel.open();
         *  //发起连接
         *  socketChannel.connect(new InetSocketAddress("localhost", 8080));
         */
        // 发送请求
//        ByteBuffer buffer = ByteBuffer.wrap("1234567890".getBytes());
//        socketChannel.write(buffer);

        //
//        buffer.clear();
        ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.put("good night".getBytes()).flip();
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

    /**
     * 监听端口
     * UDP 和 TCP 不一样，DatagramChannel 一个类处理了服务端和客户端
     * UDP 是面向无连接的，不需要和对方握手，不需要通知对方，就可以直接将数据包投出去，至于能不能送达，它是不知道的
     */
    @Test
    public void DatagramChannelReceiver() throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(8088));
        ByteBuffer buffer = ByteBuffer.allocate(5);
        channel.receive(buffer);
        String s = new String(buffer.array()).trim();
    }

    /**
     * 发送数据
     */
    @Test
    public void DatagramChannelSender() throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put("hello".getBytes());
        buffer.flip();
        channel.send(buffer, new InetSocketAddress("localhost",8088));
    }

    @Test
    public void fileChannelRead() throws Exception {
        //读取文件数据需要创建输入流
        FileInputStream fileInputStream = new FileInputStream(new File("F:\\server\\resource\\data.txt"));
        FileChannel fileChannel = fileInputStream.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        //将文件数据写入到buffer中
        int num = fileChannel.read(byteBuffer);
        String s = new String(byteBuffer.array()).trim();
        fileInputStream.close();
    }

    @Test
    public void fileChannelWrite() throws Exception {
        //数据写入文件需要创建输出流
        FileOutputStream fileInputStream = new FileOutputStream(new File("F:\\server\\resource\\data.txt"));
        FileChannel fileChannel = fileInputStream.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.put("随便写点数据到Buffer中".getBytes());
        byteBuffer.flip();
        while(byteBuffer.hasRemaining()){
            //将buffer中的数据写入到文件中
            fileChannel.write(byteBuffer);
        }
        fileInputStream.close();
    }
}
