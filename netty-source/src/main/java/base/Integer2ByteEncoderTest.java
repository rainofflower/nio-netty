package base;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * 整数编码器测试
 */
@Slf4j
public class Integer2ByteEncoderTest {

    @Test
    public void test(){
        ChannelInitializer<EmbeddedChannel> channelInitializer = new ChannelInitializer<EmbeddedChannel>() {
            protected void initChannel(EmbeddedChannel ch) throws Exception {
                ch.pipeline().addLast(new Integer2ByteEncoder());
            }
        };
        EmbeddedChannel channel = new EmbeddedChannel(channelInitializer);
        for(int i = 0; i<100; i++){
            channel.write(i);
        }
        channel.flush();
        ByteBuf msg = (ByteBuf) channel.readOutbound();
        while(msg != null){
            log.info("receive integer: {}",msg.readInt());
            msg = (ByteBuf) channel.readOutbound();
        }
    }
}
