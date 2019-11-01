package base;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class OutPipeline {

    /**
     * 测试 pipeline流水线出站 处理流程
     */
    @Test
    public void testPipelineOutBound(){
        ChannelInitializer<EmbeddedChannel> channelInitializer = new ChannelInitializer<EmbeddedChannel>() {
            protected void initChannel(EmbeddedChannel ch) throws Exception {
                ch.pipeline()
                        .addLast(new SimpleOutboundHandlerA())
                        .addLast(new SimpleOutboundHandlerB())
                        .addLast(new SimpleOutboundHandlerC());
            }
        };
        EmbeddedChannel channel = new EmbeddedChannel(channelInitializer);
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(1);
        //向通道写一个出站报文
        channel.writeOutbound(buf);
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            //
        }
    }

    static class SimpleOutboundHandlerA extends ChannelOutboundHandlerAdapter{

        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            log.info("出站处理器A 被调用");
            super.write(ctx, msg, promise);
        }
    }

    static class SimpleOutboundHandlerB extends ChannelOutboundHandlerAdapter{

        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            log.info("出站处理器B 被调用");
            super.write(ctx, msg, promise);
        }
    }

    static class SimpleOutboundHandlerC extends ChannelOutboundHandlerAdapter{

        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            log.info("出站处理器C 被调用");
            //如果不调用父类的write方法，流水线不往 前 传递
            //super.write(ctx, msg, promise);
        }
    }
}
