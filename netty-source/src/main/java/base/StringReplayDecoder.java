package base;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * ReplayingDecoder实现字符串分包解码器
 *
 * 由于字符串长度不是固定的，因此需要确定传输协议（字符串解析和程序使用的具体传输协议是强相关的）
 * 此处使用普通的 Header-Content 传输协议：
 * 1、在协议的head部分放置字符串的字节长度，可以用一个整型int来描述
 * 2、在协议的content部分，放置的是字符串字节数组
 *
 * 关于分包传输：
 * 底层通信协议是分包传输的，一份数据可能分几次达到对端。发送端出去的包传输过程中会进行多次的拆分和组装。
 * 接收端所收到的包和发送端所发送的包不是一模一样的。
 */
public class StringReplayDecoder extends ReplayingDecoder<StringReplayDecoder.State> {

    enum State{
        PARSE_1,PARSE_2
    }

    private int length;
    private byte[] content;

    StringReplayDecoder(){
        super(State.PARSE_1);
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch(state()){
            case PARSE_1:
                length = in.readInt();
                content = new byte[length];
                checkpoint(State.PARSE_2);
                break;
            case PARSE_2:
                in.readBytes(content, 0 ,length);
                out.add(new String(content,"utf-8"));
                checkpoint(State.PARSE_1);
                break;
            default:
                break;
        }
    }
}
