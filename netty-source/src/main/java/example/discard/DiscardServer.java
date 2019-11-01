package example.discard;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * Discards any incoming data.
 */
public final class DiscardServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8200"));

    public static void main(String[] args) throws Exception {
        //创建反应器线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            //设置反应器线程组
            b.group(bossGroup, workerGroup)
             //设置NIO类型的的通道
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             //装配子通道流水线
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 //有连接到达时会创建一个通道
                 public void initChannel(SocketChannel ch) {
                     ChannelPipeline p = ch.pipeline();
                     //流水线管理子通道中的handler处理器
                     //向子通道流水线添加一个Handler处理器
                     p.addLast(new DiscardServerHandler());
                 }
             });

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(PORT).sync();

            // Wait until the server socket is closed.
            //服务监听通道一直等到通道关闭的异步任务结束
            f.channel().closeFuture().sync();
        } finally {
            //释放资源
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
