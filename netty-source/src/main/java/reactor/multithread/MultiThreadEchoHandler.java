package reactor.multithread;

import lombok.extern.slf4j.Slf4j;
import util.CompositeThreadPoolConfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

@Slf4j
public class MultiThreadEchoHandler implements Runnable{

    //多线程共用byteBuffer会有并发问题，此处为压测用
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    final SocketChannel socketChannel;
    final SelectionKey sk;
    static ExecutorService pool = new CompositeThreadPoolConfig().threadPoolExecutor();
//    private Object lock = new Object();

    MultiThreadEchoHandler(MultiThreadEchoServerReactor.SubReactor reactor, SocketChannel socketChannel) throws IOException {
        this.socketChannel = socketChannel;
        socketChannel.configureBlocking(false);
        Selector selector = reactor.selector;
        while(!(MultiThreadEchoServerReactor.SubReactor.STATE.compareAndSet(reactor,
                        MultiThreadEchoServerReactor.SubReactor.SELECT ,
                        MultiThreadEchoServerReactor.SubReactor.REGISTER)
                ||
                MultiThreadEchoServerReactor.SubReactor.STATE.compareAndSet(reactor,
                        MultiThreadEchoServerReactor.SubReactor.FREE ,
                        MultiThreadEchoServerReactor.SubReactor.REGISTER)));
        try{
            /*
             * 唤醒selector.select()，释放SelectorImpl中的publicKeys实例的监视器锁
             *
             * SelectableChannel的register(Selector selector, ...)和Selector的select()方法都会操作Selector对象的共享资源publicKeys集合.
             * SelectableChannel及Selector的实现对操作共享资源的代码块进行了同步,从而避免了对共享资源的竞争.
             * 同步机制使得一个线程执行SelectableChannel的register(Selector selctor, ...)时,
             * 不允许另一个线程同时执行Selector的select()方法,反之亦然.
             *
             */
            selector.wakeup();
            //仅仅取得选择键，稍后设置感兴趣的 IO 事件
            sk = socketChannel.register(selector, 0);
        }finally {
            MultiThreadEchoServerReactor.SubReactor.STATE.compareAndSet(reactor,
                    MultiThreadEchoServerReactor.SubReactor.REGISTER ,
                    MultiThreadEchoServerReactor.SubReactor.FREE);
        }
        //将handler处理器作为选择键的附件
        sk.attach(this);
        //注册read就绪事件
        sk.interestOps(SelectionKey.OP_READ);
    }

    public void run() {
        try{
            pool.execute(new AsyncTask());
            //log.info("handler线程池信息："+pool);
        }catch (Exception e){
            log.error("[Handler] error "+e.getMessage());
        }
    }

    public void asyncRun(){
        try{
            //高并发时如果为每个业务处理开辟一个byteBuffer将成为性能瓶颈
            //如果使用池化方式，需要增加额外管理
            //ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            int length;
            //从通道读
            while((length = socketChannel.read(byteBuffer)) > 0){
                log.info("收到数据："+new String(byteBuffer.array(),0,length));
            }
            //读完后，准备写入通道，byteBuffer切换为读模式
            byteBuffer.flip();

            //写入通道
            socketChannel.write(byteBuffer);
            //shutdownOutput用于测试服务器性能，接收到数据立即返回响应
            //多次进行数据传输时不应该关闭输出
            socketChannel.shutdownOutput();
            //写完后，准备开始从通道读，byteBuffer切换为写模式
            byteBuffer.clear();
            //处理结束了，这里不能关闭select key，需要重复使用
            //sk.cancel();
        }catch (Exception e){
            //e.printStackTrace();
            //log.error("[EchoHandler] error "+e.getMessage());
            if(socketChannel != null){
                try {
                    socketChannel.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    class AsyncTask implements Runnable{
        public void run() {
            MultiThreadEchoHandler.this.asyncRun();
        }
    }
}
