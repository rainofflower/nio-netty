package base;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class ByteBufApi {

    @Test
    public void testByteBuf(){
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer(9, 100);
        print("分配ByteBuf(9,100)",buffer);
        buffer.writeBytes(new byte[]{1,2,3,4});
        print("写入4个字节（1,2,3,4）",buffer);
        //取字节，不改变读指针readerIndex
        for(int i = 0; i<buffer.readableBytes(); i++){
            log.info("取一个字节：{}",buffer.getByte(i));
        }
        print("ByteBuf取数据",buffer);
        while(buffer.isReadable()){
            log.info("读一个字节：{}",buffer.readByte());
        }
        print("ByteBuf读数据",buffer);
    }

    /**
     * Netty的内存回收工作是通过引用计数的方式来管理的。
     * 采用"计数器" refCnt 来跟踪ByteBuf 的生命周期
     */
    @Test
    public void testRef(){
        ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
        log.info("after create:{}",buffer.refCnt());
        buffer.retain();
        log.info("after retain:{}",buffer.refCnt());
        buffer.release();
        log.info("after release:{}",buffer.refCnt());
        buffer.release();
        log.info("after release:{}",buffer.refCnt());
        //会抛出异常。当refCnt为 0 时不能再retain
        buffer.retain();
        log.info("after retain:{}",buffer.refCnt());
    }

    public void print(String msg,ByteBuf buffer){
        log.info(msg+"\n" +
                        "isReadable():{}\n"+
                        "readerIndex():{}\n"+
                        "readableBytes():{}\n"+
                        "====================\n"+
                        "isWritable():{}\n"+
                        "writerIndex():{}\n"+
                        "writableBytes():{}\n"+
                        "====================\n"+
                        "capacity():{}\n"+
                        "maxCapacity():{}\n"+
                        "maxWritableBytes():{}\n",
                        buffer.isReadable(),
                        buffer.readerIndex(),
                        buffer.readableBytes(),
                        buffer.isWritable(),
                        buffer.writerIndex(),
                        buffer.writableBytes(),
                        buffer.capacity(),
                        buffer.maxCapacity(),
                        buffer.maxWritableBytes());
    }

}
