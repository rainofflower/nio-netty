package example.echo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class EchoClientHandler extends ChannelInboundHandlerAdapter {

    static final EchoClientHandler INSTANCE = new EchoClientHandler();

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf)msg;
        int length = byteBuf.readableBytes();
        byte[] data = new byte[length];
        byteBuf.getBytes(byteBuf.readerIndex(), data);
        log.info("\n 收到数据：{}", new String(data,"utf-8"));
        byteBuf.release();
    }
}
