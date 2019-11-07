package serialize.protobuf;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtobufBusinessDecoder extends ChannelInboundHandlerAdapter {

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
        MsgProtos.Msg protoMsg = (MsgProtos.Msg)msg;
        log.info("收到数据：id = {} ; content = {}",protoMsg.getId(),protoMsg.getContent());
    }
}
