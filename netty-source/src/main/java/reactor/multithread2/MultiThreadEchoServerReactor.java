package reactor.multithread2;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 反应器模式 多线程版本的服务端
 */
@Slf4j
public class MultiThreadEchoServerReactor {

    Selector listenerSelector;

    ServerSocketChannel serverSocketChannel;

    volatile boolean running = true;

    AtomicInteger next = new AtomicInteger(0);

    int reactorNum;

    SubReactor[] subReactors;

    static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors() << 1;


    AtomicLong acceptCount = new AtomicLong(0);


    MultiThreadEchoServerReactor(int port,int reactorNum) throws IOException {
        this.reactorNum = reactorNum;
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        listenerSelector = Selector.open();
        serverSocketChannel.register(listenerSelector, SelectionKey.OP_ACCEPT);
    }

    public void startService() throws IOException {
        reactorNum = reactorNum == 0 ? DEFAULT_THREADS : reactorNum;
        subReactors = new SubReactor[reactorNum];
        for(int i = 0; i < reactorNum; i++){
            subReactors[i] = new SubReactor();
            new Thread(subReactors[i],"Reactor-"+i).start();
        }
        while (running){
            try {
                int select = listenerSelector.select();
                if(select == 0){
                    continue;
                }
                Iterator<SelectionKey> it = listenerSelector.selectedKeys().iterator();
                while (it.hasNext()){
                    SelectionKey sk = it.next();
                    it.remove();
                    if(sk.isValid() && sk.isAcceptable()){
                        doAccept(sk);
                    }
                }
            } catch (IOException e) {
                log.error("startService error", e);
            }
        }
    }

    private void doAccept(SelectionKey key){
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel;
        try{
            while((socketChannel = serverSocketChannel.accept()) != null) {
                log.info("{} 收到新连接,总共收到连接数：{}",Thread.currentThread().getName(),acceptCount.incrementAndGet());
                SubReactor subReactor = subReactors[next.get()];
                try {
                    subReactor.startAdd();
                    new MultiThreadEchoHandler(subReactor, socketChannel);
                }finally {
                    subReactor.finishAdd();
                }
            }
        } catch (IOException e) {
            log.error("[Acceptor] error ", e);
        }
        if(next.incrementAndGet() == subReactors.length){
            next.set(0);
        }
    }

    class SubReactor implements Runnable{
        final Selector selector;

        volatile boolean adding;

        SubReactor() throws IOException {
            selector = Selector.open();
        }

        public void run() {
            while(running){
                try {
                    selector.select();

                    while (adding){
                        synchronized(this) {
                            this.wait(1000);
                        }
                    }

                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> it = selectionKeys.iterator();
                    while(it.hasNext()){
                        SelectionKey sk = it.next();
                        it.remove();
                        if(sk.isValid() && sk.isReadable()){
                            dispatch(sk);
                        }
                    }
                }catch (IOException e){
                    log.error("[SubReactor] error", e);
                }catch (InterruptedException e) {
                    log.error("[SubReactor] error", e);
                }
            }
        }

        //添加新的任务时，阻塞当前任务
        public void startAdd() {
            adding = true;
            selector.wakeup();
        }

        public synchronized void finishAdd() {
            adding = false;
            this.notify();
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
}
