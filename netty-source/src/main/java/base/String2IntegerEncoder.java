package base;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class String2IntegerEncoder extends MessageToMessageEncoder<String> {

    protected void encode(ChannelHandlerContext ctx, String msg, List<Object> out) throws Exception {
        char[] chars = msg.toCharArray();
        for(char a : chars){
            //48是0的编码，57是9的编码
            if(a >= 48 && a <= 57){
                out.add(new Integer(a));
            }
        }
    }
}
