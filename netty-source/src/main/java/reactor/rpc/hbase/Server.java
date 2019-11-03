package reactor.rpc.hbase;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by wangkai8 on 17/1/5.
 */
public class Server {

    public static final Logger LOG = LogManager.getLogger();

    //读取来自客户端数据的阻塞队列
    private BlockingQueue<Call> queue = new LinkedBlockingQueue<Call>();

    //处理未写完的任务的阻塞队列
    private Queue<Call> responseCalls = new ConcurrentLinkedQueue<Call>();

    volatile boolean running = true;

    //处理向客户端写任务的对象
    private Responder responder = null;
    //缓冲器读写策略的阀值
    private static int NIO_BUFFER_LIMIT = 64 * 1024;

    private int handler = 10;


    /**
     * 负责服务器的初始化，实例化serverSocketChannel，和selector,包括serverSocketChannel绑定ip地址和端口,
     * 并向通道注册accept事件。启动多个reader线程，用于后续和客户端连接通讯
     * 
     *
     */
    class Listener extends Thread {

        Selector selector;
        Reader[] readers;
        int robin;
        int readNum;

        Listener(int port) throws IOException {
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(port), 150);
            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            readNum = 10;
            readers = new Reader[readNum];
            for(int i = 0; i < readNum; i++) {
                readers[i] = new Reader(i);
                readers[i].start();
            }
        }


        public void run() {
            while(running) {
                try {
                    selector.select();
                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while(it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();
                        if(key.isValid()) {
                            if(key.isAcceptable()) {
                                doAccept(key);
                            }
                        }
                    }
                } catch (IOException e) {
                    LOG.error("", e);
                }
            }
        }

        //处理客户端连接事件
        public void doAccept(SelectionKey selectionKey) throws IOException {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
            SocketChannel socketChannel;
            while((socketChannel = serverSocketChannel.accept()) != null) {
                try {
                    //设置为非阻塞的模式
                    socketChannel.configureBlocking(false);
                    socketChannel.socket().setTcpNoDelay(true);
                    socketChannel.socket().setKeepAlive(true);
                } catch (IOException e) {
                    socketChannel.close();
                    throw e;
                }
                //取模的方式获取reader对象用于处理后续操作
                Reader reader = getReader();
                try {
                    //添加新的任务时，阻塞当前reader的任务  
                    //这添加同步代码块的原因应该是，为了防止connection对象，没构造完或者还没被添加到附件时，而读事件却触发了、
                    //所以添加同步的机制，当添加新的任务时，阻塞当前读取任务的执行。
                    reader.startAdd();
                    //注册可读事件
                    SelectionKey readKey = reader.registerChannel(socketChannel);
                    //构造connection对象
                    Connection c = new Connection(socketChannel);
                    //添加到该SelectionKey对象的附件中
                    readKey.attach(c);
                } finally {
                    //添加完成任务，唤醒被阻塞reader对象
                    reader.finishAdd();
                }
            }
        }

        public Reader getReader() {
            //防止超过int的最大值
            if(robin == Integer.MAX_VALUE) {
                robin = 0;
            }
            return readers[(robin ++) % readNum];
        }
    }

    //用来处理客户端的可读事件
    class Reader extends Thread {

        Selector readSelector;
        boolean adding;

        Reader(int i) throws IOException {
            setName("Reader-" + i);
            this.readSelector = Selector.open();
            LOG.info("Starting Reader-" + i + "...");
        }

        @Override
        public void run() {
            //循环处理读事件
            while(running) {
                try {
                    //
                    readSelector.select();

                    //这添加同步代码块的原因应该是，为了防止connection对象，没构造完，而读事件却触发了、
                    while(adding) {
                        synchronized(this) {
                            this.wait(1000);
                        }
                    }

                    Iterator<SelectionKey> it = readSelector.selectedKeys().iterator();
                    while(it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();
                        if(key.isValid()) {
                            if(key.isReadable()) {
                                doRead(key);
                            }
                        }
                    }
                } catch (IOException e) {
                    LOG.error("", e);
                } catch (InterruptedException e) {
                    LOG.error("", e);
                }
            }
        }

        //具体的读操作方法
        public void doRead(SelectionKey selectionKey) {
            //获取关联的connection对象
            Connection c = (Connection) selectionKey.attachment();
            if(c == null) {
                return;
            }

            int n;
            try {
                n = c.readAndProcess();
            } catch (IOException e) {
                LOG.error("", e);
                n = -1;
            } catch (Exception e) {
                LOG.error("", e);
                n = -1;
            }
            if(n == -1) {
                c.close();
            }
        }

        //socketChannel向readSelector注册可读事件
        public SelectionKey registerChannel(SocketChannel channel) throws IOException {
            return channel.register(readSelector, SelectionKey.OP_READ);
        }

        //添加新的任务时，阻塞当前任务  
        public void startAdd() {
            adding = true;
            readSelector.wakeup();
        }

        public synchronized void finishAdd() {
            adding = false;
            this.notify();
        }
    }


    class Connection {
        private SocketChannel channel;
        //用来保存客户端内容的长度
        private ByteBuffer dataBufferLength;
        //用来读取客户端具体的内容
        private ByteBuffer dataBuffer;
        private boolean skipHeader;

        public Connection(SocketChannel channel) {
            this.channel = channel;
            //为dataBufferLength为4个字节的缓冲器
            this.dataBufferLength = ByteBuffer.allocate(4);
        }

        public int readAndProcess() throws IOException {

            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            int length;
            //从通道读
            while((length = channel.read(byteBuffer)) > 0){
                LOG.info("收到数据："+new String(byteBuffer.array(),0,length));
            }
            channel.shutdownOutput();
            int count = 0;
            //第一次获取，要读的内容的长度，并将值存于dataBufferLength缓冲器中，int值，就4个字节，
//            if(!skipHeader) {
//                count = channelRead(channel, dataBufferLength);
//                if (count < 0 || dataBufferLength.remaining() > 0) {
//                    return count;
//                }
//            }
//
//            skipHeader = true;
//            //如果dataBuffer为null就为dataBuffer初始化，分配的字节长度为dataBufferLength读到的值
//            if(dataBuffer == null) {
//                dataBufferLength.flip();
//                int dataLength = dataBufferLength.getInt();
//                dataBuffer = ByteBuffer.allocate(dataLength);
//            }
//
//            count = channelRead(channel, dataBuffer);
//            //如果读取的数据不为0,并且没有剩余的长度，说明读取完成，执行下一步操作
//            if(count >= 0 && dataBuffer.remaining() == 0) {
//                process();
//            }

            return count;
        }


        /**
         * process the dataBuffer
         */
        public void process() {
            dataBuffer.flip();
            byte[] data = dataBuffer.array();
            //构造Call对象，并加入到阻塞队列
            Call call = new Call(this, data, responder);
            try {
                queue.put(call);
            } catch (InterruptedException e) {
                LOG.error("", e);
            }

        }


        public void close() {
            if(channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                }
            }
        }
    }

    //向客户端回写数据
    class Responder extends Thread {

        Selector writeSelector;

        public Responder() throws IOException {
            writeSelector = Selector.open();
        }

        public void run() {
            //循环处理写事件
            while(running) {
                try {
                    registWriters();
                    int n = writeSelector.select(1000);
                    if(n == 0) {
                        continue;
                    }
                    Iterator<SelectionKey> it = writeSelector.selectedKeys().iterator();
                    while(it.hasNext()) {
                        SelectionKey key = it.next();
                        it.remove();
                        if(key.isValid() && key.isWritable()) {
                            doAsyncWrite(key);
                        }
                    }
                } catch (IOException e) {
                    LOG.error("", e);
                }
            }
        }

        //从阻塞队列中获取，call对象，并为该对象中的socketChannel注册写事件
        public void registWriters() throws IOException {
            Iterator<Call> it = responseCalls.iterator();
            while(it.hasNext()) {
                Call call = it.next();
                it.remove();
                SelectionKey key = call.conn.channel.keyFor(writeSelector);
                try {
                    if (key == null) {
                        try {
                            //注册写事件，并将call对象作为附件
                            call.conn.channel.register(writeSelector, SelectionKey.OP_WRITE, call);
                        } catch (ClosedChannelException e) {
                            //the client went away
                            if (LOG.isTraceEnabled())
                                LOG.trace("the client went away", e);
                        }
                    } else {
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                } catch (CancelledKeyException e) {
                    if (LOG.isTraceEnabled())
                        LOG.trace("the client went away", e);
                }
            }
        }

        //将Call对象添加至写的阻塞队列
        public void registerForWrite(Call call) throws IOException {
            responseCalls.add(call);
            writeSelector.wakeup();
        }

        private void doAsyncWrite(SelectionKey key) throws IOException {
            Call call = (Call) key.attachment();
            if(call.conn.channel != key.channel()) {
                throw new IOException("bad channel");
            }
            //将内容写回客户端
            int numBytes = channelWrite(call.conn.channel, call.response);
            if(numBytes < 0 || call.response.remaining() == 0) {
                try {
                    key.interestOps(0);
                } catch (CancelledKeyException e) {
                    LOG.warn("Exception while changing ops : " + e);
                }
            }
        }

        private void doResponse(Call call) throws IOException {
            //if data not fully send, then register the channel for async writer
            //如果数据没有完全发送完，就作为任务加入待发送的阻塞队列
            if(!processResponse(call)) {
                //没写完就加入写会的阻塞队列
                registerForWrite(call);
            }
        }

        //将call的buffer内容写回客户端，如果全部写完，则返回true，如果未全部写完，则返回false,之后未写完的，作为任务加入至阻塞队列。
        private boolean processResponse(Call call) throws IOException {
            boolean error = true;
            try {
                int numBytes = channelWrite(call.conn.channel, call.response);
                if (numBytes < 0) {
                    throw new IOException("error socket write");
                }
                error = false;
            } finally {
                if(error) {
                    call.conn.close();
                }
            }
            if(!call.response.hasRemaining()) {
                call.done = true;
                return true;
            }
            return false;
        }
    }
    //处理queue队列中的任务。
    class Handler extends Thread {

        public Handler(int i) {
            setName("handler-" + i);
            LOG.info("Starting Handler-" + i + "...");
        }

        public void run() {
            while(running) {
                try {
                    Call call = queue.take();
                    process(call);
                } catch (InterruptedException e) {
                    LOG.error("", e);
                } catch (IOException e) {
                    LOG.error("", e);
                }
            }
        }

        public void process(Call call) throws IOException {
            //打印从客户端收到的数据
            byte[] request = call.request;
            String message = new String(request);
            LOG.info("received mseesage: " + message);

            //each channel write 2MB data for test
            int dataLength = 2 * 1024 * 1024;
            ByteBuffer buffer = ByteBuffer.allocate(4 + dataLength);

            buffer.putInt(dataLength);
            writeDataForTest(buffer);
            buffer.flip();

            //将buffer赋给call对象的response，为写回做准备
            call.response = buffer;
            responder.doResponse(call);
        }
    }
    //为了测试填充数据，前4个字节为，回写客户端数据的字节长度
    public void writeDataForTest(ByteBuffer buffer) {
        int n = buffer.limit() - 4;
        for(int i = 0; i < n; i++) {
            buffer.put((byte)0);
        }
    }


    class Call {
        Connection conn;
        byte[] request;
        Responder responder;
        ByteBuffer response;
        boolean done;
        public Call(Connection conn, byte[] request, Responder responder) {
            this.conn = conn;
            this.request = request;
            this.responder = responder;
        }
    }

    //当前buffer剩余的长度少于64M，就将数据直接读入buffer，如果大于64M，则循环读入（估计是为了防止一次性读太多，引起系统的开销过大，所以循环读）
    public int channelRead(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        return buffer.remaining() <= NIO_BUFFER_LIMIT ? channel.read(buffer) : channleIO(channel, null, buffer);
    }

    public int channelWrite(WritableByteChannel channel, ByteBuffer buffer) throws IOException {
        return buffer.remaining() <= NIO_BUFFER_LIMIT ? channel.write(buffer) : channleIO(null, channel, buffer);
    }


    public int channleIO(ReadableByteChannel readCh, WritableByteChannel writeCh, ByteBuffer buffer) throws IOException {
        int initRemaining = buffer.remaining();
        int originalLimit = buffer.limit();

        int ret = 0;
        try {
            while (buffer.remaining() > 0) {
                int ioSize = Math.min(buffer.remaining(), NIO_BUFFER_LIMIT);
                buffer.limit(buffer.position() + ioSize);
                ret = readCh == null ? writeCh.write(buffer) : readCh.read(buffer);
                if (ret < ioSize) {
                    break;
                }
            }
        } finally {
            buffer.limit(originalLimit);
        }

        int byteRead = initRemaining - buffer.remaining();
        return byteRead > 0 ? byteRead : ret;
    }


    public void startHandler() {
        for(int i = 0; i < handler; i++) {
            new Handler(i).start();
        }
    }


    public void start() throws IOException {
        new Listener(8200).start();
        responder = new Responder();
        responder.start();
        startHandler();
        LOG.info("server startup! ");
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start();
    }
}