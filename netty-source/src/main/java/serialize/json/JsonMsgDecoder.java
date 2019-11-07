package serialize.json;

import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonMsgDecoder extends ChannelInboundHandlerAdapter {

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception{
        String json = (String)msg;
        JsonMsg jsonMsg = JSONObject.parseObject(json, JsonMsg.class);
        log.info("收到一个json数据包：{}",jsonMsg.toString());
    }
}
