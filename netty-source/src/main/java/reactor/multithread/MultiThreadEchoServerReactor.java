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
                /**
                 * state锁未被占用才允许执行selector.select()
                 * 此处加锁原因：
                 * 如果查询IO可读事件线程中的selector 和
                 * 监听连接 线程在接收到新连接之后根据选择策略选中的selector是同一个selector，
                 * 那么查询IO可读的操作-执行selector.select()方法 会阻塞 收到连接之后注册可读事件的操作-执行SelectableChannel.register(Selector selector, ...)方法
                 * 因为同一个selector在执行以上两个操作都会获取selector中的publicKeys监视器锁，而selectors.select()方法本身又是个阻塞的方法，
                 * 一旦执行了selectors.select(),获取到publicKeys监视器锁之后并且阻塞了，锁还未释放之前，执行channel的register就会因为获取不到锁也发生阻塞
                 *
                 * 结合自己的这份多线程reactor的实现来说，就是除了监听连接事件的subReactors[0],其它的subReactor线程执行select方法会阻塞主线程，
                 * 也就是监听连接事件的线程subReactor[0]执行选中的selector去注册通道可读事件。
                 * 具体点：监听连接事件的子反应器线程执行new MultiThreadEchoHandler(subReactors[next.get()],channel)方法，使用next.get()来选择selector，
                 * 被选中的 selector 可能已经在包含该 selector 实例的子反应器线程中执行selector.select()方法，此时 new MultiThreadEchoHandler(subReactors[next.get()],channel)
                 * 方法中的 sk = socketChannel.register(selector, 0)就会被阻塞。
                 *
                 * 另外，subReactor[0]中的selector本身同时用来select和注册可读事件不会发生cas争锁的问题，因为两者在同一个线程中执行
                 */
                if(STATE.compareAndSet(this,FREE,SELECT)){
                    try {
                        int keyCount = selector.select();
                        //log.info("{} 已收到 key的数量：{}",Thread.currentThread().getName(),selectedKeyCount.incrementAndGet());
                        if(keyCount == 0){
                            //log.info("selector: {}  发生中断或者调用了warkup()方法",selector);
                            continue;
                        }
                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        Iterator<SelectionKey> it = selectionKeys.iterator();
                        while(it.hasNext()){
                            SelectionKey sk = it.next();
                            if(sk.isValid()){
                                if(sk.isAcceptable() || sk.isReadable())
                                dispatch(sk);
                            }
                        }
                        selectionKeys.clear();
                    }catch (IOException e){
                        log.error("[Selector] error "+e.getMessage());
                    }finally {
                        //如果锁状态未被其它线程修改，自己得释放
                        STATE.compareAndSet(this,SELECT,FREE);
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
