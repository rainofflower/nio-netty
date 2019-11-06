package base;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 使用ReplayingDecoder，借助其长度检测免去自己编写长度判断代码
 */
@Slf4j
public class Byte2IntegerReplayDecoder extends ReplayingDecoder {

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int i = in.readInt();
        log.info("解码出一个整数：{}",i);
        out.add(i);
    }
}
