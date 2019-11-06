package base;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * 整数编码器
 *
 * 编码器基类MessageToByteEncoder，将java对象转为ByteBuf数据包
 * 子类需要实现抽象的encode()编码方法
 */
@Slf4j
public class Integer2ByteEncoder extends MessageToByteEncoder<Integer> {

    protected void encode(ChannelHandlerContext ctx, Integer msg, ByteBuf out) throws Exception {
        out.writeInt(msg);
        log.info("encoder integer = {}",msg);
    }
}
