package serialize.protobuf;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class ProtobufDemo {

    /**
     * 第 1 种方式：序列化 Serialization & 反序列化 Deserialization
     */
    @Test
    public void serializationAndDeser1() throws IOException {
        MsgProtos.Msg msg = buildMsg();
        //将 Protobuf 对象序列化为二进制字节数组
        byte[] data = msg.toByteArray();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(data);
        data = outputStream.toByteArray();
        MsgProtos.Msg inMsg = MsgProtos.Msg.parseFrom(data);
        log.info("收到数据：id = {} ; content = {}",inMsg.getId(),inMsg.getContent());
    }

    /**
     * 第 2 种方式：序列化 Serialization & 反序列化 Deserialization
     * 该方式仅使用于阻塞式二进制码流传输的场景中（如java OIO、写出到文件）
     * 在异步操作的NIO场景中，存在半包、粘包问题
     */
    @Test
    public void serializationAndDeser2() throws IOException {
        MsgProtos.Msg msg = buildMsg();
        //序列化到二进制码流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        msg.writeTo(outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        //从二进制码流反序列化成Protobuf对象
        MsgProtos.Msg inMsg = MsgProtos.Msg.parseFrom(inputStream);
        log.info("收到数据：id = {} ; content = {}",inMsg.getId(),inMsg.getContent());
    }

    /**
     * 第 3 种方式：序列化 Serialization & 反序列化 Deserialization
     * 带字节长度：[字节长度] [字节数据] ，解决半包、粘包问题
     */
    @Test
    public void serializationAndDeser3() throws IOException {
        MsgProtos.Msg msg = buildMsg();
        //序列化到二进制码流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        msg.writeDelimitedTo(outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        //从二进制码流反序列化成Protobuf对象
        MsgProtos.Msg inMsg = MsgProtos.Msg.parseDelimitedFrom(inputStream);
        log.info("收到数据：id = {} ; content = {}",inMsg.getId(),inMsg.getContent());
    }



    public MsgProtos.Msg buildMsg(){
        MsgProtos.Msg.Builder builder = MsgProtos.Msg.newBuilder();
        builder.setId(1000);
        builder.setContent("高性能学习之Protobuf");
        MsgProtos.Msg msg = builder.build();
        return msg;
    }
}
