package example.echo;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;
import org.springframework.util.StringUtils;

import java.util.Scanner;

public class EchoClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    @Test
    public void client() throws Exception{
        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
    //                            if (sslCtx != null) {
    //                                p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
    //                            }
                        //p.addLast(new LoggingHandler(LogLevel.INFO));
                        p.addLast(EchoClientHandler.INSTANCE);
                    }
                });
            // Start the client.
            ChannelFuture f = b.connect(HOST, PORT).sync();
            Channel channel = f.channel();
            Scanner scanner = new Scanner(System.in);
            System.out.println("请输入发送内容：");
            while(scanner.hasNext()){
                String next = scanner.next();
                if(!StringUtils.isEmpty(next) && next.equals("exit")){
                    break;
                }
                byte[] bytes = next.getBytes("utf-8");
                ByteBuf buffer = channel.alloc().buffer();
                buffer.writeBytes(bytes);
                channel.writeAndFlush(buffer);
                System.out.println("请输入发送内容：");
            }
            // Wait until the connection is closed.
//            f.channel().closeFuture().sync();
        } finally {
            //优雅关闭
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }

    public static void main(String... args){
        try {
            new EchoClient().client();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
