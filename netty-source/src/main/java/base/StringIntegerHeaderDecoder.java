package base;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 继承ByteToMessageDecoder基类，实现Header-Content协议传输分包的字符串内容解码器
 *
 * ReplayingDecoder在数据解析逻辑复杂的场景中解析速度相对较差，
 * 建议自己实现ByteToMessageDecoder基类或者其子类
 */
public class StringIntegerHeaderDecoder extends ByteToMessageDecoder {

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //可读字节数小于Integer字节数（4），消息头还没读满，返回
        if(in.readableBytes() < Integer.BYTES){
            return;
        }
        //消息头已经读完整
        //在真正开始从缓冲区读取数据之前，调用markReaderIndex()设置回滚点
        //回滚点为消息头的readerIndex读指针位置
        in.markReaderIndex();
        int length = in.readInt();
        //从缓冲区读取消息头的数据大小，这会使readerIndex读指针前移
        //剩余长度不够消息体，重置读指针
        if(in.readableBytes() < length){
            //读指针回滚到消息头的readerIndex的位置，未进行状态保存
            in.resetReaderIndex();
            return;
        }
        //消息体数据齐了，读取数据，编码成字符串
        byte[] content = new byte[length];
        in.readBytes(content, 0 ,length);
        out.add(new String(content,"utf-8"));
    }
}
