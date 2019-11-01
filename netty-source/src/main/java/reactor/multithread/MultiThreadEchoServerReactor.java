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
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 反应器模式 多线程版本的服务端
 */
@Slf4j
public class MultiThreadEchoServerReactor {

    ServerSocketChannel serverSocketChannel;

    AtomicInteger next = new AtomicInteger(0);

    SubReactor[] subReactors;

    static final int DEFAULT_THREADS = Runtime.getRuntime().availableProcessors() << 1;

//    long selectorTimeout = 1000;

    AtomicLong acceptCount = new AtomicLong(0);

//    volatile int keyCount = 0;

    MultiThreadEchoServerReactor(int port,int reactorNum) throws IOException {
        reactorNum = reactorNum == 0 ? DEFAULT_THREADS : reactorNum;
        subReactors = new SubReactor[reactorNum];
        for(int i = 0; i<reactorNum; i++){
            subReactors[i] = new SubReactor();
        }
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        SelectionKey sk = serverSocketChannel.register(subReactors[0].selector, SelectionKey.OP_ACCEPT);
        sk.attach(new AcceptorHandler());
    }

    public void startService(){
        for(int i = 0; i<subReactors.length; i++){
            new Thread(subReactors[i],"Reactor-"+i).start();
        }
    }



    static class SubReactor implements Runnable{
        final Selector selector;
        //锁状态： 未使用
        static final int FREE = 0;
        //锁状态： 被通道注册独占
        static final int REGISTER = 1;
        //锁状态：调用了select方法
        static final int SELECT = 2;
        /**
         * selector状态锁，控制并发
         * 防止通道注册到selector和selector调用select发生死锁
         * 一个selector对应一把锁
         */
        volatile int state;
        static final AtomicIntegerFieldUpdater<SubReactor> STATE = AtomicIntegerFieldUpdater.newUpdater(SubReactor.class,"state");
        AtomicLong selectedKeyCount = new AtomicLong(0);

        SubReactor() throws IOException {
            selector = Selector.open();
        }

        public void run() {
            while(!Thread.interrupted()){
                //state锁未被占用才允许执行selector.select()
                while(STATE.compareAndSet(this,FREE,SELECT)){
                    try {
                        int keyCount = selector.select();
                        //如果锁状态未被其它线程修改，自己得释放
                        STATE.compareAndSet(this,SELECT,FREE);
                        log.info("{} 已收到 key的数量：{}",Thread.currentThread().getName(),selectedKeyCount.incrementAndGet());
                        if(keyCount == 0){
                            //log.info("selector: {}  发生中断或者调用了warkup()方法",selector);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        Iterator<SelectionKey> it = selectionKeys.iterator();
                        while(it.hasNext()){
                            SelectionKey sk = it.next();
                            dispatch(sk);
                        }
                        selectionKeys.clear();
                    }catch (IOException e){
                        log.error("[Selector] error "+e.getMessage());
                    }
                }
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
                log.info("{} 收到新连接,总共收到连接数：{}",Thread.currentThread().getName(),acceptCount.incrementAndGet());
                if(channel != null){
                    new MultiThreadEchoHandler(subReactors[next.get()],channel);
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("[Acceptor] error "+e.getMessage());
            }
            if(next.incrementAndGet() == subReactors.length){
                next.set(0);
            }
        }
    }
}
