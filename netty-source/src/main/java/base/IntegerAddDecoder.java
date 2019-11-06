package base;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * ReplayingDecoder实现整数分包解码器
 */
public class IntegerAddDecoder extends ReplayingDecoder<IntegerAddDecoder.State> {

    enum State{
        PARSE_1,PARSE_2
    }

    int first;
    int second;

    IntegerAddDecoder(){
        super(State.PARSE_1);
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch(state()){
            case PARSE_1:
                first = in.readInt();
                checkpoint(State.PARSE_2);
                break;
            case PARSE_2:
                second = in.readInt();
                int sum = first + second;
                out.add(sum);
                checkpoint(State.PARSE_1);
                break;
            default:
                break;
        }
    }
}
