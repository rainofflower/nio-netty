package reactor.multithread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

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

    Selector[] selectors = null;

    SubReactor[] subReactors = null;

    MultiThreadEchoServerReactor(int port,int reactorNum) throws IOException {
        if(reactorNum == 0){
            log.error("反应器线程数不能等于0");
            return;
        }
        selectors = new Selector[reactorNum];
        for(int i = 0; i<reactorNum; i++){
            selectors[i] = Selector.open();
        }
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        SelectionKey sk = serverSocketChannel.register(selectors[0], SelectionKey.OP_ACCEPT);
        sk.attach(new AcceptorHandler());
        subReactors = new SubReactor[reactorNum];
        for(int i = 0; i<reactorNum; i++){
            subReactors[i] = new SubReactor(selectors[i]);
        }
    }

    public void startService(){
        for(int i = 0; i<subReactors.length; i++){
            new Thread(subReactors[i],"Reactor-"+i).start();
        }
    }



    class SubReactor implements Runnable{
        final Selector selector;

        SubReactor(Selector selector){
            this.selector = selector;
        }

        public void run() {
            try {
                while(!Thread.interrupted()){
                    int count = selector.select();
                    if(count == 0){
                        log.info("selector: {}  发生中断或者调用了warkup()方法",selector);
                        continue;
                    }
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> it = selectionKeys.iterator();
                    while(it.hasNext()){
                        SelectionKey sk = it.next();
                        dispatch(sk);
                    }
                    selectionKeys.clear();
                }
            }catch (IOException e){
                log.error("[Selector] error "+e.getMessage());
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
            } catch (Exception e) {
                log.error("[Acceptor] error "+e.getMessage());
            }
            if(next.incrementAndGet() == selectors.length){
                next.set(0);
            }
        }
    }
}
