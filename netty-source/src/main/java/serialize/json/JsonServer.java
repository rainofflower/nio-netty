package serialize.json;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import org.junit.Test;

import java.nio.charset.Charset;

/**
 * json协议通信（json反序列化） -服务端
 */
public class JsonServer {

    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    @Test
    public void server() throws Exception{
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    /**
                                     * 入站解码
                                     * 先使用LengthFieldBasedFrameDecoder解码出Header-Content二进制数据包中的content二进制内容
                                     * 然后使用StringDecoder将二进制内容解码成字符串
                                     * 最后使用JsonMsgDecoder将json字符串解码为java pojo对象
                                     */
                                    .addLast(new LengthFieldBasedFrameDecoder(1024,0,4,0,4))
                                    .addLast(new StringDecoder(Charset.forName("utf-8")))
                                    .addLast(new JsonMsgDecoder());
                        }
                    });
            ChannelFuture f = b.bind(PORT).sync();
            f.channel().closeFuture().sync();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
