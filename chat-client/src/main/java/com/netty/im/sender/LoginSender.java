package com.netty.im.sender;

import com.netty.im.bean.msg.ProtoMsg;
import com.netty.im.protoBuilder.LoginMsgBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("LoginSender")
public class LoginSender extends BaseSender {


    public void sendLoginMsg() {
        if (!isConnected()) {
            log.info("还没有建立连接!");
            return;
        }
        log.info("构造登录消息");
        ProtoMsg.Message message =
                LoginMsgBuilder.buildLoginMsg(getUser(), getSession());
        log.info("发送登录消息");
        super.sendMsg(message);
    }


}
