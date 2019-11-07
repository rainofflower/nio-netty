package serialize.protobuf;

import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import serialize.json.JsonMsg;

import java.nio.charset.Charset;

/**
 * json协议通信（json序列化） - 客户端
 */
@Slf4j
public class ProtoBufClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    @Test
    public void client() throws Exception{
        NioEventLoopGroup group = new NioEventLoopGroup();
        try{
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                /**
                                 *  出站编码
                                 *  从后往前执行，因此添加顺序是反的
                                 *  先使用 ProtobufEncoder 编码器将ProtoBuf对象编码成二进制字节数组
                                 *  然后使用 ProtobufVarint32LengthFieldPrepender编码器将二进制字节数组编码成Header-Content格式的二进制数据包
                                 */
                                .addLast(new ProtobufVarint32LengthFieldPrepender())
                                .addLast(new ProtobufEncoder());
                        }
                    });
            ChannelFuture f = b.connect(HOST, PORT).sync();
            Channel channel = f.channel();
            for(int i = 0; i<1000; i++){
                MsgProtos.Msg msg = buildMsg(i, "啦啦啦啦啦啦,学习Protobuf啊");
                channel.writeAndFlush(msg);
                log.info("发送报文：{}",msg);
            }
            channel.flush();
            channel.closeFuture().sync();
        }finally {
            group.shutdownGracefully();
        }
    }

    public MsgProtos.Msg buildMsg(int id, String content){
        MsgProtos.Msg.Builder builder = MsgProtos.Msg.newBuilder();
        builder.setId(id);
        builder.setContent(content);
        return builder.build();
    }

}
