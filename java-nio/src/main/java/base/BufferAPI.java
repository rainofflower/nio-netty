package base;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Buffer 数据读写与常用方法
 */
@Slf4j
public class BufferAPI {

    private int position = 0;

    /**
     * 初始化buffer的常用方式
     */
    @Test
    public void init(){
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        log.info(buffer.position() + " "+buffer.limit() +" "+buffer.capacity()+" "+buffer.get());

        ByteBuffer wrapBuffer = ByteBuffer.wrap("hello".getBytes());
        log.info(wrapBuffer.position() + " "+wrapBuffer.limit() +" "+wrapBuffer.capacity()+" "+new String(new byte[]{wrapBuffer.get()}));
    }

    /**
     * 如果要读 Buffer 中的值，需要切换模式，从写入模式切换到读出模式。
     * 注意，通常在说 NIO 的读操作的时候，我们说的是从 Channel 中读数据到 Buffer 中，对应的是对 Buffer 的写入操作
     *
     *
     * clear和compact的区别
     *  clear() 方法会重置几个属性，但是我们要看到，clear() 方法并不会将 Buffer 中的数据清空，只不过后续的写入会覆盖掉原来的数据，也就相当于清空了数据了。
     * 而 compact() 方法有点不一样，调用这个方法以后，会先处理还没有读取的数据，也就是 position 到 limit 之间的数据（还没有读过的数据），先将这些数据移到左边，然后在这个基础上再开始写入。
     * 很明显，此时 limit 还是等于 capacity，position 指向原来数据的右边。
     */
    @Test
    public void test1(){
        int i = incrementAfter(position);
        int j = incrementBefore(position);
        int a = increment();
        IntBuffer buffer = IntBuffer.allocate(1024);
        buffer.put(2);
        buffer.put(new int[]{1,2});
        buffer.flip();
        int b = buffer.get();
        int[] array = buffer.array();
        ByteBuffer byteBuffer = ByteBuffer.allocate(10);
        byteBuffer.put(new byte[]{65,127});
        String s = new String(byteBuffer.array()).trim();
        //flip写入模式切换到读取模式（其实就是设置一下position和limit）
        byteBuffer.flip();
        byte b1 = byteBuffer.get();
        //mark方法结合reset使用，mark之后调用reset可以回到mark时的地方读取buffer的值
        byteBuffer.mark();
        byte b2 = byteBuffer.get();
        //回到mark点读取
        byteBuffer.reset();
        byte b3 = byteBuffer.get();

        //rewind重置position为0，通常用于从头读或写buffer
        byteBuffer.rewind();
        //clear重置buffer,其实就是将position设置为0，将limit设置为capacity，重置mark
        //一般重新往buffer中填充数据之前调用clear
        byteBuffer.clear();
        //compact和clear一样，都是在准备往buffer中填充数据之前调用，但是两者还是有区别的，具体看上方说明
        byteBuffer.compact();

    }

    public int incrementAfter(int i){
        return i++;
    }

    public int incrementBefore(int i){
        return ++i;
    }

    public int increment(){
        //return position++;
        return ++position;
    }
}
