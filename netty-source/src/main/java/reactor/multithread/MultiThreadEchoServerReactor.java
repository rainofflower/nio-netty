package reactor.multithread;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MultiThreadEchoServerReactor {

    ServerSocketChannel serverSocketChannel;

    AtomicInteger next = new AtomicInteger(0);

    Selector[] selectors = new Selector[2];

    SubReactor[] subReactors = null;

    MultiThreadEchoServerReactor(int port) throws IOException {
        selectors[0] = Selector.open();
        selectors[1] = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        SelectionKey sk = serverSocketChannel.register(selectors[0], SelectionKey.OP_ACCEPT);
        sk.attach(new AcceptorHandler());
        SubReactor subReactor1 = new SubReactor(selectors[0]);
        SubReactor subReactor2 = new SubReactor(selectors[1]);
        subReactors = new SubReactor[]{subReactor1, subReactor2};
    }



    class SubReactor implements Runnable{
        final Selector selector;

        SubReactor(Selector selector){
            this.selector = selector;
        }

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
                log.error(e.getLocalizedMessage());
            }
        }

        /**
         * 反应器分发事件
         */
        void dispatch(SelectionKey sk){
            Runnable handler = (Runnable)sk.attachment();
            //调用之前绑定到选择键上的handler处理器
            if(handler != null){
                handler.run();
            }
        }
    }

    class AcceptorHandler implements Runnable{

        public void run() {
            try{
                SocketChannel channel = serverSocketChannel.accept();
                log.info("收到新连接："+channel);
                if(channel != null){
                    new MultiThreadEchoHandler(selectors[next.get()],channel);
                }
            } catch (IOException e) {

            }
            if(next.incrementAndGet() == selectors.length){
                next.set(0);
            }

        }
    }
}
