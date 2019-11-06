package base;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

/**
 * 解码器测试
 */
public class Byte2BasicDataDecoderTest {

    @Test
    public void test(){
        ChannelInitializer<EmbeddedChannel> channelInitializer = new ChannelInitializer<EmbeddedChannel>() {

            protected void initChannel(EmbeddedChannel ch) throws Exception {
                ch.pipeline()
                        //.addLast(new Byte2BasicDataDecoder(Long.class)) //Character.class
                        //.addLast(new Byte2IntegerReplayDecoder())
                        .addLast(new IntegerAddDecoder())
                        .addLast(new BasicDataProcessHandler());
            }
        };
        EmbeddedChannel channel = new EmbeddedChannel(channelInitializer);
        for(int i = 0;i<10;i++){
            ByteBuf buffer = Unpooled.buffer();
            buffer.writeInt(i);
            channel.writeInbound(buffer);
        }
//        for(long i = 0;i<100;i++){
//            ByteBuf buffer = Unpooled.buffer();
//            buffer.writeLong(i);
//            channel.writeInbound(buffer);
//        }
//        ByteBuf buffer1 = Unpooled.buffer();
//        buffer1.writeChar('a');
//        channel.writeInbound(buffer1);
//        ByteBuf buffer2 = Unpooled.buffer();
//        buffer2.writeChar('b');
//        channel.writeInbound(buffer2);
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            //
        }
    }
}
