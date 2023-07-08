package reactor.multithread2;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

@Slf4j
public class MultiThreadEchoHandler implements Runnable{

    final SocketChannel socketChannel;

    MultiThreadEchoHandler(MultiThreadEchoServerReactor.SubReactor reactor, SocketChannel socketChannel) throws IOException {
        //设置为非阻塞的模式
        socketChannel.configureBlocking(false);
        socketChannel.socket().setTcpNoDelay(true);
        socketChannel.socket().setKeepAlive(true);
        this.socketChannel = socketChannel;
        Selector selector = reactor.selector;
        //注册可读事件
        SelectionKey sk = socketChannel.register(selector, SelectionKey.OP_READ);
        //将handler处理器作为选择键的附件
        sk.attach(this);
    }

    public void run() {
        try{
            io();
            //log.info("handler线程池信息："+pool);
        }catch (Exception e){
            log.error("[Handler] error", e);
        }
    }

    public void io(){
        try{
            //高并发时如果为每个业务处理开辟一个byteBuffer将成为性能瓶颈
            //如果使用池化方式，需要增加额外管理
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
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
//            socketChannel.shutdownOutput();
        }catch (Exception e){
            log.error("io error", e);
            if(socketChannel != null){
                try {
                    socketChannel.close();
                } catch (IOException e1) {
                    log.error("socketChannel close error", e);
                }
            }
        }
    }

}
