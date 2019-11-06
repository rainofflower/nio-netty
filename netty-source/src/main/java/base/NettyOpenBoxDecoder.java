package base;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Random;

/**
 * Netty内置的解码器
 */
public class NettyOpenBoxDecoder {

    static String delimiter = "\t";
    static String content = "高性能学习之Netty";
    static int VERSION = 100;
    static int MAGICCODE = 123321;

    /**
     * DelimiterBasedFrameDecoder 解码器
     */
    @Test
    public void testDelimiterBasedFrameDecoder(){
        ByteBuf delimiterBuf = Unpooled.copiedBuffer(delimiter.getBytes(Charset.forName("UTF-8")));
        ChannelInitializer<EmbeddedChannel> channelInitializer = new ChannelInitializer<EmbeddedChannel>() {
            protected void initChannel(EmbeddedChannel ch) throws Exception {
                ch.pipeline()
                        .addLast(new DelimiterBasedFrameDecoder(1024, true, delimiterBuf))
                        .addLast(new StringDecoder())
                        .addLast(new StringProcessHandler());
            }
        };
        EmbeddedChannel channel = new EmbeddedChannel(channelInitializer);
        byte[] bytes = content.getBytes(Charset.forName("UTF-8"));
        Random random = new Random();
        for(int i = 0; i<100; i++){
            int num = random.nextInt(3) + 1;
            ByteBuf buffer = Unpooled.buffer();
            for(int j = 0; j<num; j++){
                buffer.writeBytes(bytes);
            }
            buffer.writeBytes(delimiter.getBytes());
            channel.writeInbound(buffer);
        }
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * LengthFieldBasedFrameDecoder 解码器
     * 解析简单的Header-Content协议数据
     * 头4字节表示内容字节长度，后面的字节数组表示数据内容
     *
     * 一般会使用该类实现Header-Content协议数据解码，而不是自己去实现ByteToMessageDecoder，比如自己实现的StringIntegerHeaderDecoder
     */
    @Test
    public void testLengthFieldBasedFrameDecoder1(){
        ChannelInitializer<EmbeddedChannel> channelInitializer = new ChannelInitializer<EmbeddedChannel>() {
            protected void initChannel(EmbeddedChannel ch) throws Exception {
                ch.pipeline()
                        .addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 0 , 4))
                        .addLast(new StringDecoder())
                        .addLast(new StringProcessHandler());
            }
        };
        EmbeddedChannel channel = new EmbeddedChannel(channelInitializer);
        byte[] bytes = content.getBytes(Charset.forName("UTF-8"));
        for(int i = 0; i<100; i++){
            ByteBuf buffer = Unpooled.buffer();
            buffer.writeInt(bytes.length);
            buffer.writeBytes(bytes);
            channel.writeInbound(buffer);
        }
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * LengthFieldBasedFrameDecoder 解码器
     * 解析包含协议版本号的Header-Content协议数据
     * 头4字节表示内容字节长度，紧接着2个字节表示版本号，后面的字节数组表示数据内容
     *
     * 长度调整值(lengthAdjustment)的计算公式：内容字段偏移量-长度字段偏移量-长度字段的字节数
     */
    @Test
    public void testLengthFieldBasedFrameDecoder2(){
        ChannelInitializer<EmbeddedChannel> channelInitializer = new ChannelInitializer<EmbeddedChannel>() {
            protected void initChannel(EmbeddedChannel ch) throws Exception {
                ch.pipeline()
                        .addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, 2 , 6))
                        .addLast(new StringDecoder())
                        .addLast(new StringProcessHandler());
            }
        };
        EmbeddedChannel channel = new EmbeddedChannel(channelInitializer);
        byte[] bytes = content.getBytes(Charset.forName("UTF-8"));
        for(int i = 0; i<100; i++){
            ByteBuf buffer = Unpooled.buffer();
            buffer.writeInt(bytes.length);
            buffer.writeChar(VERSION);
            buffer.writeBytes(bytes);
            channel.writeInbound(buffer);
        }
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * LengthFieldBasedFrameDecoder 解码器
     * 解析包含协议版本号、魔数的Header-Content协议数据
     * 数据包描述：前面是2个字节的版本数据，然后是4字节长度数据，紧接着4个字节的魔数，
     * 用来对数据包做一些安全的认证，后面的字节表示数据内容content
     *
     * 长度调整值(lengthAdjustment)的计算公式：内容字段偏移量-长度字段偏移量-长度字段的字节数
     */
    @Test
    public void testLengthFieldBasedFrameDecoder3(){
        ChannelInitializer<EmbeddedChannel> channelInitializer = new ChannelInitializer<EmbeddedChannel>() {
            protected void initChannel(EmbeddedChannel ch) throws Exception {
                ch.pipeline()
                        .addLast(new LengthFieldBasedFrameDecoder(1024, 2, 4, 4 , 10))
                        .addLast(new StringDecoder())
                        .addLast(new StringProcessHandler());
            }
        };
        EmbeddedChannel channel = new EmbeddedChannel(channelInitializer);
        byte[] bytes = content.getBytes(Charset.forName("UTF-8"));
        for(int i = 0; i<100; i++){
            ByteBuf buffer = Unpooled.buffer();
            buffer.writeChar(VERSION);
            buffer.writeInt(bytes.length);
            buffer.writeInt(MAGICCODE);
            buffer.writeBytes(bytes);
            channel.writeInbound(buffer);
        }
        try {
            Thread.sleep(Integer.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
