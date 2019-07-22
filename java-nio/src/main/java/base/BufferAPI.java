package base;

import org.junit.Test;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Buffer 数据读写与常用方法
 */
public class BufferAPI {

    private int position = 0;

    @Test
    public void init(){
        Buffer buffer = ByteBuffer.allocate(1024);
        ByteBuffer wrapBuffer = ByteBuffer.wrap(new byte[1024]);
    }

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
        byteBuffer.flip();
        byte b1 = byteBuffer.get();
        byteBuffer.mark();
        byte b2 = byteBuffer.get();
        byteBuffer.reset();
        byte b3 = byteBuffer.get();

        byteBuffer.rewind();
        byteBuffer.clear();
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
