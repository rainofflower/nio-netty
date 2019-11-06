package base;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BasicDataProcessHandler extends ChannelInboundHandlerAdapter {

    public void channelRead(ChannelHandlerContext cx,Object msg) throws Exception{
        log.info("打印出一个结果：{}",msg);
        super.channelRead(cx,msg);
    }
}
