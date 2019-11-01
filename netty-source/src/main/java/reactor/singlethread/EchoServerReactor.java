package reactor.singlethread;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reactor反应器 单线程版
 */
@Slf4j
public class EchoServerReactor implements Runnable{

    final Selector selector;

    final ServerSocketChannel serverSocketChannel;

    AtomicLong count = new AtomicLong(0);

    EchoServerReactor(int port) throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        //注册ServerSocket的accept事件
        SelectionKey sk = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        //将acceptorHandler处理器作为附件，绑定到sk选择键
        sk.attach(new AcceptorHandler());
    }

    /**
     * 选择器轮询，分发事件
     */
    public void run() {
        try {
            while(!Thread.interrupted()){
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                while(it.hasNext()){
                    SelectionKey sk = it.next();
                    dispatch(sk);
                }
                selectionKeys.clear();
            }
        }catch (IOException e){

        }
    }

    /**
     * 反应器分发事件
     */
    public void dispatch(SelectionKey sk){
        Runnable handler = (Runnable)sk.attachment();
        //调用之前绑定到选择键上的handler处理器
        if(handler != null){
            handler.run();
        }
    }

    /**
     * Handler:新连接处理器
     * 内部类，共用ServerSocketChannel和Selector
     */
    class AcceptorHandler implements Runnable{

        public void run() {
            try {
                //接收新连接
                SocketChannel socketChannel = serverSocketChannel.accept();
                log.info("已收到连接数：{}",count.incrementAndGet());
                if(socketChannel != null){
                    new EchoHandler(socketChannel,selector);
                }
            } catch (IOException e) {

            }
        }
    }

}
