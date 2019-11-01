package base;

import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InboundHandler extends ChannelInboundHandlerAdapter {

    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        log.info("handlerAdded 被回调");
    }

    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        log.info("handlerRemoved 被回调");
    }

    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        log.info("channelRegistered 被回调");
        //super.channelRegistered(ctx);
        ctx.fireChannelRegistered();
    }

    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        log.info("channelUnregistered 被回调");
        ctx.fireChannelUnregistered();
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("channelActive 被回调");
        ctx.fireChannelActive();
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("channelInactive 被回调");
        ctx.fireChannelInactive();
    }


    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.info("channelRead 被回调");
        ctx.fireChannelRead(msg);
    }


    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        log.info("channelReadComplete 被回调");
        ctx.fireChannelReadComplete();
    }


    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log.info("userEventTriggered 被回调");
        ctx.fireUserEventTriggered(evt);
    }


    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        log.info("channelWritabilityChanged 被回调");
        ctx.fireChannelWritabilityChanged();
    }


    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        log.info("exceptionCaught 被回调");
        ctx.fireExceptionCaught(cause);
    }
}
