package base;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Random;

public class StringIntegerHeaderDecoderTest {

    @Test
    public void test(){
        ChannelInitializer<EmbeddedChannel> channelInitializer = new ChannelInitializer<EmbeddedChannel>() {
            protected void initChannel(EmbeddedChannel ch) throws Exception {
                ch.pipeline()
                        .addLast(new StringIntegerHeaderDecoder())
                        .addLast(new StringProcessHandler());
            }
        };
        EmbeddedChannel channel = new EmbeddedChannel(channelInitializer);
        String content = "高性能学习:netty.";
        byte[] bytes = content.getBytes(Charset.forName("UTF-8"));
        Random random = new Random();
        for(int i = 0; i<100; i++){
            int num = random.nextInt(3) + 1;
            ByteBuf buffer = Unpooled.buffer();
            buffer.writeInt(bytes.length * num);
            for(int j = 0; j<num; j++){
                buffer.writeBytes(bytes);
            }
            channel.writeInbound(buffer);
        }
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
