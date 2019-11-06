package base;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 编解码器（看源码可以看出其是通过继承（成员属性继承，继承过来的方法转为调用成员decode和encode方法）编码器和解码器实现的）
 */
@Slf4j
public class Byte2IntegerCodec extends ByteToMessageCodec<Integer> {

    protected void encode(ChannelHandlerContext ctx, Integer msg, ByteBuf out) throws Exception {
        out.writeInt(msg);
        log.info("encode data:{}",msg);
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(in.readableBytes() >= 4) {
            int i = in.readInt();
            log.info("decode data:{}", i);
            out.add(i);
        }
    }
}
