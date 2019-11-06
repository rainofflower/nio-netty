package base;

import io.netty.channel.CombinedChannelDuplexHandler;

/**
 * 比继承方式实现编解码更灵活的组合编码解码类 CombinedChannelDuplexHandler
 */
public class IntegerDuplexHandler extends CombinedChannelDuplexHandler {

    IntegerDuplexHandler(){
        super(new Byte2BasicDataDecoder(Integer.class), new Integer2ByteEncoder());
    }
}
