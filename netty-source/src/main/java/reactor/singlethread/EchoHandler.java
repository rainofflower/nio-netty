package reactor.singlethread;

import com.sun.org.apache.regexp.internal.RE;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Handler处理器
 */
@Slf4j
public class EchoHandler implements Runnable {

    final SocketChannel socketChannel;
    final SelectionKey sk;
    final ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    static final int RECEIVINT = 0, SENDING = 1;
    int state = RECEIVINT;

    EchoHandler(SocketChannel socketChannel, Selector selector) throws IOException {
        this.socketChannel = socketChannel;
        socketChannel.configureBlocking(false);
        //仅仅取得选择键，稍后设置感兴趣的 IO 事件
        sk = socketChannel.register(selector, 0);
        //将handler处理器作为选择键的附件
        sk.attach(this);
        //注册read就绪事件
        sk.interestOps(SelectionKey.OP_READ);
        selector.wakeup();
    }

    public void run() {
        try{
            if(state == SENDING){
                //写入通道
                socketChannel.write(byteBuffer);
                //写完后，准备开始从通道读，byteBuffer切换为写模式
                byteBuffer.clear();
                //写完后，注册read就绪事件
                sk.interestOps(SelectionKey.OP_READ);
                //写完后，进入接收的状态
                state = RECEIVINT;
            }
            else if(state == RECEIVINT){
                int length = 0;
                //从通道读
                while((length = socketChannel.read(byteBuffer)) > 0){
                    log.info("收到数据："+new String(byteBuffer.array()).trim());
                }
                //读完后，准备写入通道，byteBuffer切换为读模式
                byteBuffer.flip();
                //读完后，注册write就绪事件
                sk.interestOps(SelectionKey.OP_WRITE);
                //读完后，进入发送状态
                state = SENDING;
            }
            //处理结束了，这里不能关闭select key，需要重复使用
            //sk.cancel();
        }catch (IOException e){

        }
    }
}
