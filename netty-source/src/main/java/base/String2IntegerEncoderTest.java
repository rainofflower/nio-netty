package base;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class String2IntegerEncoderTest {

    @Test
    public void test(){
        ChannelInitializer<EmbeddedChannel> channelInitializer = new ChannelInitializer<EmbeddedChannel>() {
            protected void initChannel(EmbeddedChannel ch) throws Exception {
                ch.pipeline()
                        .addLast(new Integer2ByteEncoder())
                        .addLast(new String2IntegerEncoder());
            }
        };
        EmbeddedChannel channel = new EmbeddedChannel(channelInitializer);
        for(int i = 0; i<100; i++){
            String s = "i am "+i;
            channel.write(s);
        }
        channel.flush();
        ByteBuf msg = (ByteBuf) channel.readOutbound();
        while(msg != null){
            log.info("receive: {}",msg.readInt());
            msg = (ByteBuf) channel.readOutbound();
        }
    }
}
