package example.echo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
/**
 * 如果一个handler是无状态的，没有与通道强相关的数据，只需操作局部变量
 * 可以声明为共性模式被多个通道的流水线共享，提升服务器性能。
 * netty默认不允许handler共享，如果不加@ChannelHandler.Sharable注解，
 * 试图将一个Handler实例添加到多个ChannelPipeline,netty将抛出异常。
 *
 * 同一个通道上的所有业务处理器，只能被同一个线程处理。所以，不是@Sharable的业务处理器，是线程安全的。
 * 而不同通道，可以绑定到不同的EventLoop反应器线程，因此，加了@Sharable的业务处理器实例，可能被多个线程并发执行，
 * 所以共享的业务处理器不是线程安全的，如果需要操作共享变量，需要另外加上同步控制。
 */
@ChannelHandler.Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    static final EchoServerHandler INSTANCE = new EchoServerHandler();

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        log.info("\n 收到数据class：{}, 缓冲区类型：{}",byteBuf.getClass(),byteBuf.hasArray()?"堆内存":"直接内存");
        int length = byteBuf.readableBytes();
        byte[] data = new byte[length];
        byteBuf.getBytes(byteBuf.readerIndex(), data);
        log.info("\n 收到数据：{}", new String(data,"utf-8"));
        log.info("写回前refCnt:{}",byteBuf.refCnt());
        ChannelFuture f = ctx.channel().writeAndFlush(msg);
        f.addListener((ChannelFuture channelFuture)->{
            log.info("写回后refCnt：{}",byteBuf.refCnt());
        });
    }
}
