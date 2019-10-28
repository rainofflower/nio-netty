package base;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import util.CompositeThreadPoolConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 异步IO
 *
 * 通常，我们会有一个线程池用于执行异步任务，提交任务的线程将任务提交到线程池就可以立马返回，
 * 不必等到任务真正完成。如果想要知道任务的执行结果，通常是通过传递一个回调函数的方式，任务结束后去调用这个函数。
 *
 * 同样的原理，Java 中的异步 IO 也是一样的，都是由一个线程池来负责执行任务，然后使用回调或自己去查询结果。
 *
 * Java 异步 IO 提供了两种使用方式，分别是返回 Future 实例和使用回调函数。
 *
 */
@Slf4j
public class AIO {

    @Test
    public void AsynchronousFileChannel() throws IOException, ExecutionException, InterruptedException {
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get("F:\\server\\resource\\data.txt"));
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        Future<Integer> result = channel.read(buffer, 0);
        result.get();
        String s = new String(buffer.array()).trim();
        log.info("读取到文件内容："+s);
    }

    @Test
    public void AsynchronousServerSocketChannel() throws IOException {
        /**
         * 实例化，并监听端口
         *  AsynchronousServerSocketChannel.open()执行完了就可以看到后台默认已经启动了cpu最大逻辑线程数了
         * （Intel 8750h 有12个逻辑线程数，因此可以看到12个工作线程），其实就是用来处理异步任务的。
         *
         *  线程池也可以自己设置
         *  只需调用AsynchronousChannelGroup 里的静态方法就能返回一个 group 了，将该group传递到open方法就可以使用自定义的线程池了
         *
         *  AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(new CompositeThreadPoolConfig().threadPoolExecutor());
         *  AsynchronousServerSocketChannel.open(asynchronousChannelGroup);
         *  AsynchronousSocketChannel.open(asynchronousChannelGroup);
         */

        AsynchronousServerSocketChannel server =
                AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(8080));

        // 自己定义一个 Attachment 类，用于传递一些信息
        Attachment att = new Attachment();
        att.setServer(server);

        server.accept(att, new CompletionHandler<AsynchronousSocketChannel, Attachment>() {

            public void completed(AsynchronousSocketChannel client, Attachment att) {
                try {
                    SocketAddress clientAddr = client.getRemoteAddress();
                    log.info("收到新的连接：" + clientAddr);

                    // 收到新的连接后，server 应该重新调用 accept 方法等待新的连接进来
                    att.getServer().accept(att, this);

                    Attachment newAtt = new Attachment();
                    newAtt.setServer(server);
                    newAtt.setClient(client);
                    newAtt.setReadMode(true);
                    newAtt.setBuffer(ByteBuffer.allocate(2048));

                    // 异步读取数据，这里也可以继续使用匿名实现类，不过代码不好看，所以这里专门定义一个类
                    client.read(newAtt.getBuffer(), newAtt, new ChannelHandler());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            public void failed(Throwable t, Attachment att) {
                System.out.println("accept failed");
            }
        });
        // 为了防止 main 线程退出
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
        }
    }

    @Getter
    @Setter
    class Attachment{
        private AsynchronousServerSocketChannel server;

        private AsynchronousSocketChannel client;

        private boolean readMode;

        private ByteBuffer buffer;
    }

    class ChannelHandler implements CompletionHandler<Integer, Attachment>{

        public void completed(Integer result, Attachment attachment) {
            if(attachment.isReadMode()){
                // 读取来自客户端的数据
                ByteBuffer buffer = attachment.getBuffer();
                buffer.flip();
                String re = new String(buffer.array()).trim();
                log.info("接收到数据："+re);

                // 响应客户端请求，返回数据
                buffer.clear();
                buffer.put("响应内容：我已收到信息".getBytes(Charset.forName("UTF-8")));
                buffer.flip();
                attachment.setReadMode(false);
                // 写数据到客户端也是异步
                attachment.getClient().write(buffer, attachment, this);
            }
            else{
                // 到这里，说明往客户端写数据也结束了，有以下两种选择:
                // 1. 继续等待客户端发送新的数据过来
//            att.setReadMode(true);
//            att.getBuffer().clear();
//            att.getClient().read(att.getBuffer(), att, this);
                // 2. 既然服务端已经返回数据给客户端，断开这次的连接
                try {
                    attachment.getClient().close();
                } catch (IOException e) {
                }
            }
        }

        public void failed(Throwable exc, Attachment attachment) {
            log.error("连接断开");
        }
    }

    @Test
    public void AsynchronousSocketChannel() throws IOException, ExecutionException, InterruptedException {
        AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
        // 来个 Future 形式的
        Future<?> future = client.connect(new InetSocketAddress("localhost",8080));
        // 阻塞一下，等待连接成功
        future.get();

        Attachment att = new Attachment();
        att.setClient(client);
        att.setReadMode(false);
        att.setBuffer(ByteBuffer.allocate(2048));
        byte[] data = "月光 把天空照亮".getBytes();
        att.getBuffer().put(data);
        att.getBuffer().flip();

        // 异步发送数据到服务端
        client.write(att.getBuffer(), att, new ClientChannelHandler());

        // 这里休息一下再退出，给出足够的时间处理数据
        Thread.sleep(2000);
    }

    class ClientChannelHandler implements CompletionHandler<Integer, Attachment>{

        public void completed(Integer result, Attachment att) {
            ByteBuffer buffer = att.getBuffer();
            if (att.isReadMode()) {
                // 读取来自服务端的数据
                buffer.flip();
                byte[] bytes = new byte[buffer.limit()];
                buffer.get(bytes);
                String msg = new String(bytes, Charset.forName("UTF-8"));
                log.info("收到来自服务端的响应数据: " + msg);

                // 接下来，有以下两种选择:
                // 1. 向服务端发送新的数据
//            att.setReadMode(false);
//            buffer.clear();
//            String newMsg = "new message from client";
//            byte[] data = newMsg.getBytes(Charset.forName("UTF-8"));
//            buffer.put(data);
//            buffer.flip();
//            att.getClient().write(buffer, att, this);
                // 2. 关闭连接
                try {
                    att.getClient().close();
                } catch (IOException e) {
                }
            } else {
                // 写操作完成后，会进到这里
                att.setReadMode(true);
                buffer.clear();
                att.getClient().read(buffer, att, this);
            }
        }

        public void failed(Throwable t, Attachment att) {
            log.error("服务器无响应");
        }
    }
}
