package example.echo;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.junit.Test;

import java.nio.charset.Charset;

/**
 * 数据发送客户端，结合EchoServer展示半包，粘包问题
 */
public class DumpSendClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "256"));

    @Test
    public void client() throws Exception{
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
                            p.addLast(EchoClientHandler.INSTANCE);
                        }
                    });
            // Start the client.
            ChannelFuture f = b.connect(HOST, PORT).sync();
            Channel channel = f.channel();
            String content = "高性能学习之Netty.";
            byte[] bytes = content.getBytes(Charset.forName("utf-8"));
            //发送1000个ByteBuf
            for(int i = 0; i<1000; i++){
                ByteBuf buffer = channel.alloc().buffer();
                buffer.writeBytes(bytes);
                channel.writeAndFlush(buffer);
            }
            channel.closeFuture().sync();
        } finally {
            //优雅关闭
            group.shutdownGracefully();
        }
    }
}
