package base;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

public class InHandlerTest {

    /**
     * 使用 EmbeddedChannel 测试 InboundHandler 生命周期
     */
    @Test
    public void testInHandlerLifeCircle(){
        ChannelInitializer<EmbeddedChannel> channelInitializer = new ChannelInitializer<EmbeddedChannel>() {
            protected void initChannel(EmbeddedChannel ch) throws Exception {
                ch.pipeline().addLast(new InboundHandler());
            }
        };
        EmbeddedChannel channel = new EmbeddedChannel(channelInitializer);
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(1);
        //向通道写一个入站报文
        channel.writeInbound(buf);
        channel.flush();
        channel.writeInbound(buf);
        channel.flush();
        channel.close();
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            //
        }
    }
}