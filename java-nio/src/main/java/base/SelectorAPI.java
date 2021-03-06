package base;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * Selector 与 非阻塞IO
 *
 * NIO 中 Selector 是对底层操作系统实现的一个抽象，管理通道状态其实都是底层系统实现的，不同操作系统底层实现不一样，
 * 比如2002 年 随 Linux 内核 2.5.44 发布的 epoll ，Windows 平台的非阻塞 IO ，
 * 但是我们只需要面向Selector编程就可以了，毕竟JVM是一个屏蔽实现底层的平台
 *
 * Selector中的几个常用方法
 *  1、select()
 *
 * 调用此方法，会将上次 select 之后的准备好的 channel 对应的 SelectionKey 复制到 selected set 中。
 * 如果没有任何通道准备好，这个方法会阻塞，直到至少有一个通道准备好。
 *
 *  2、selectNow()
 *
 * 功能和 select 一样，区别在于如果没有准备好的通道，那么此方法会立即返回 0。
 *
 *  3、select(long timeout)
 *
 * 看了前面两个，这个应该很好理解了，如果没有通道准备好，此方法会等待一会
 *
 *  4、wakeup()
 *
 * 这个方法是用来唤醒等待在 select() 和 select(timeout) 上的线程的。
 * 如果 wakeup() 先被调用，此时没有线程在 select 上阻塞，那么之后的一个 select() 或 select(timeout) 会立即返回，而不会阻塞，
 * 当然，它只会作用一次。
 */
@Slf4j
public class SelectorAPI {

    /**
     * 非阻塞IO
     *
     * 非阻塞 IO 的核心在于使用一个 Selector 来管理多个通道，
     * 可以是 SocketChannel，也可以是 ServerSocketChannel，将各个通道注册到 Selector 上，指定监听的事件。
     * 之后可以只用一个线程来轮询这个 Selector，看看上面是否有通道是准备好的，当通道准备好可读或可写，
     * 然后才去开始真正的读写，这样速度就很快了。我们就完全没有必要给每个通道都起一个线程。
     *
     * Non-Blocking IO 示例，客户端使用ChannelAPI里的SocketChannel即可
     */
    @Test
    public void Selector() throws IOException {
        Selector selector = Selector.open();

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(8080));
        //将通道设置为非阻塞模式，因为默认都是阻塞模式的
        serverSocketChannel.configureBlocking(false);
        // 将其注册到 Selector 中，监听 OP_ACCEPT 事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while(true){
            //selector的select方法是个阻塞方法，具体看select方法源码上的注释
            int readyChannels = selector.select();
            if(readyChannels == 0){
                continue;
            }
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            while(iterator.hasNext()){
                SelectionKey selectionKey = iterator.next();
                iterator.remove();
                if(selectionKey.isAcceptable()){
                    ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel();
                    // 有已经接受的新的到服务端的连接
                    SocketChannel socketChannel = server.accept();
                    if(socketChannel == null){
                        continue;
                    }
                    // 有新的连接并不代表这个通道就有数据，
                    // 这里将这个新的 SocketChannel 注册到 Selector，监听 OP_READ 事件，等待数据
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                }
                else if(selectionKey.isReadable()){
                    // 有数据可读
                    // 上面一个 if 分支中注册了监听 OP_READ 事件的 SocketChannel
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    try{
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int num = socketChannel.read(buffer);
                        if(num > 0){
                            // 处理进来的数据
                            String msg = new String(buffer.array()).trim();
                            log.info("收到数据："+msg);
                            ByteBuffer response = ByteBuffer.wrap(("服务端返回数据: "+msg).getBytes());
                            socketChannel.write(response);
                            socketChannel.shutdownOutput();
                        }
                        else if(num == -1){
                            // -1 代表连接已经关闭
                            log.info("连接已关闭");
                            socketChannel.close();
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                        if(socketChannel!=null){
                            socketChannel.close();
                        }
                    }
                }
            }

        }
    }
}
