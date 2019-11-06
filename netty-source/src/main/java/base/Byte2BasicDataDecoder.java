package base;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 解码器 byte->java基础数据类型
 */
@Slf4j
public class Byte2BasicDataDecoder extends ByteToMessageDecoder {

    Class clazz;

    Byte2BasicDataDecoder(Class clazz){
        this.clazz = clazz;
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(clazz == Short.class){
            while(in.readableBytes() >= Short.BYTES){
                int i = in.readShort();
                log.info("解码出一个整数：{}",i);
                out.add(i);
            }
        }
        else if(clazz == Integer.class){
            while(in.readableBytes() >= Integer.BYTES){
                int i = in.readInt();
                log.info("解码出一个整数：{}",i);
                out.add(i);
            }
        }
        else if(clazz == Long.class){
            while(in.readableBytes() >= Long.BYTES){
                long i = in.readLong();
                log.info("解码出一个整数：{}",i);
                out.add(i);
            }
        }
        else if(clazz == Character.class){
            while(in.readableBytes() >= Character.BYTES){
                char i = in.readChar();
                log.info("解码出一个字符：{}",i);
                out.add(i);
            }
        }
    }
}
