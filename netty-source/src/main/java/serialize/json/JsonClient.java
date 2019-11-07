package serialize.json;

import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.nio.charset.Charset;

/**
 * json协议通信（json序列化） - 客户端
 */
@Slf4j
public class JsonClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));

    @Test
    public void client() throws Exception{
        NioEventLoopGroup group = new NioEventLoopGroup();
        try{
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                /**
                                 *  出站编码
                                 *  从后往前执行，因此添加顺序是反的
                                 *  先使用StringEncoder编码器将json字符串编码成二进制字节数组
                                 *  然后使用LengthFieldPrepender编码器将二进制字节数组编码成Header-Content格式的二进制数据包
                                 */
                                .addLast(new LengthFieldPrepender(4,false))
                                .addLast(new StringEncoder(Charset.forName("utf-8")));
                        }
                    });
            ChannelFuture f = b.connect(HOST, PORT).sync();
            Channel channel = f.channel();
            for(int i = 0; i<1000; i++){
                JsonMsg msg = buildJsonMsg(i, "啦啦啦啦啦啦,学习netty啊");
                String json = JSONObject.toJSONString(msg);
                channel.writeAndFlush(json);
                log.info("发送报文：{}",json);
            }
            channel.flush();
            channel.closeFuture().sync();
        }finally {
            group.shutdownGracefully();
        }
    }

    public JsonMsg buildJsonMsg(long id,String content){
        JsonMsg jsonMsg = new JsonMsg();
        jsonMsg.setId(id);
        jsonMsg.setContent(content);
        return jsonMsg;
    }

}
