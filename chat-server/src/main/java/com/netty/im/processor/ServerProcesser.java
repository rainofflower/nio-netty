package com.netty.im.processor;

import com.netty.im.bean.msg.ProtoMsg;
import com.netty.im.server.ServerSession;

/**
 * 操作类
 */
public interface ServerProcesser {

    ProtoMsg.HeadType type();

    boolean action(ServerSession ch, ProtoMsg.Message proto);

}
