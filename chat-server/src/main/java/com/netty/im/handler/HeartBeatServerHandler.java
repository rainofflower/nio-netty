package com.netty.im.handler;

import com.netty.im.bean.msg.ProtoMsg;
import com.netty.im.server.ServerSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import util.SimpleThreadPool;

import java.util.concurrent.TimeUnit;

/**
 * 心跳检测 -- 服务端空闲检测
 */
@Slf4j
public class HeartBeatServerHandler extends IdleStateHandler {

    private static final int DEFAULT_READ_IDLE_GAP = 150;

    public HeartBeatServerHandler(int readIdleGap){
        super(readIdleGap, 0 ,0 , TimeUnit.SECONDS);
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
        if(msg == null || !(msg instanceof ProtoMsg.Message)){
            super.channelRead(ctx, msg);
        }
        ProtoMsg.Message pkg = (ProtoMsg.Message)msg;
        ProtoMsg.HeadType type = pkg.getType();
        if(type.equals(ProtoMsg.HeadType.HEART_BEAT)){
            //异步发送心跳包
            SimpleThreadPool.getInstance().execute(()->{
                if(ctx.channel().isActive()){
                    ctx.writeAndFlush(msg);
                }
            });
        }
        super.channelRead(ctx,msg);
    }

    /**
     * 限定时间内未收到数据会回调该方法
     */
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        log.info("{} 秒内未读取到数据，关闭连接，释放资源");
        ServerSession.closeSession(ctx);
    }
}
