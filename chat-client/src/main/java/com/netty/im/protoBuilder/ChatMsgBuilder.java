package com.netty.im.protoBuilder;

import com.netty.im.bean.ChatMsg;
import com.netty.im.bean.User;
import com.netty.im.bean.msg.ProtoMsg;
import com.netty.im.client.ClientSession;

/**
 * 聊天消息Builder
 */

public class ChatMsgBuilder extends BaseBuilder {


    private ChatMsg chatMsg;
    private User user;


    public ChatMsgBuilder(ChatMsg chatMsg, User user, ClientSession session) {
        super(ProtoMsg.HeadType.MESSAGE_REQUEST, session);
        this.chatMsg = chatMsg;
        this.user = user;

    }


    public ProtoMsg.Message build() {
        ProtoMsg.Message message = buildCommon(-1);
        ProtoMsg.MessageRequest.Builder cb
                = ProtoMsg.MessageRequest.newBuilder();

        chatMsg.fillMsg(cb);
        return message
                .toBuilder()
                .setMessageRequest(cb)
                .build();
    }

    public static ProtoMsg.Message buildChatMsg(
            ChatMsg chatMsg,
            User user,
            ClientSession session) {
        ChatMsgBuilder builder =
                new ChatMsgBuilder(chatMsg, user, session);
        return builder.build();

    }
}