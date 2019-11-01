package base;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class InPipeline {

    /**
     * 测试 pipeline流水线 入站处理流程
     */
    @Test
    public void testPipelineInBound(){
        ChannelInitializer<EmbeddedChannel> channelInitializer = new ChannelInitializer<EmbeddedChannel>() {
            protected void initChannel(EmbeddedChannel ch) throws Exception {
                ch.pipeline()
                        .addLast(new SimpleInboundHandlerA())
                        .addLast(new SimpleInboundHandlerB())
                        .addLast(new SimpleInboundHandlerC());
            }
        };
        EmbeddedChannel channel = new EmbeddedChannel(channelInitializer);
        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(1);
        //向通道写一个入站报文
        channel.writeInbound(buf);
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            //
        }
    }

    static class SimpleInboundHandlerA extends ChannelInboundHandlerAdapter{

        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            log.info("入站处理器A 被回调");
            //如果不执行父类的channelRead()方法（父类实际上执行的是fireChannelRead()方法），数据在流水线中的不会再往 后 传递
            super.channelRead(ctx,msg);
        }
    }

    static class SimpleInboundHandlerB extends ChannelInboundHandlerAdapter{

        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            log.info("入站处理器B 被回调");
            super.channelRead(ctx,msg);
        }
    }

    static class SimpleInboundHandlerC extends ChannelInboundHandlerAdapter{

        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            log.info("入站处理器C 被回调");
            super.channelRead(ctx,msg);
        }
    }
}
