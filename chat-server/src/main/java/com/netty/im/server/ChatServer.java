package com.netty.im.server;

import com.netty.im.codec.ProtoBufDecoder;
import com.netty.im.codec.ProtoBufEncoder;
import com.netty.im.handler.ChatRedirectHandler;
import com.netty.im.handler.HeartBeatServerHandler;
import com.netty.im.handler.LoginRequestHandler;
import com.netty.im.handler.ServerExceptionHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Data
@Slf4j
@Service
public class ChatServer implements ApplicationRunner {

    @Value("${server.port}")
    private int port;

    @Value("${netty.boss-threads}")
    private int bossThreads;

    @Value("${netty.worker-threads}")
    private int workerThreads;

    @Value("${heartbeat.read-idle-gap}")
    private int readIdleGap;

    @Autowired
    private LoginRequestHandler loginRequestHandler;

    @Autowired
    private ChatRedirectHandler chatRedirectHandler;

    @Autowired
    private ServerExceptionHandler serverExceptionHandler;

    public void run(ApplicationArguments args){
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(bossThreads);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(workerThreads);
        try{
            ServerBootstrap b = new ServerBootstrap();
            ChannelFuture f = b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new ProtoBufDecoder())
                                    .addLast(new ProtoBufEncoder())
                                    .addLast(new HeartBeatServerHandler(readIdleGap))
                                    .addLast(loginRequestHandler)
                                    .addLast(chatRedirectHandler)
                                    .addLast(serverExceptionHandler);
                        }
                    })
                    .bind(port);
            f.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(future.isSuccess()){
                        log.info("IM 服务已启动,端口号：{}",future.channel().localAddress());
                    }
                    else{
                        log.error("IM 服务启动失败");
                        future.cause().printStackTrace();
                    }
                }
            });
            //监听通道关闭
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            //优雅关闭EventLoopGroup，释放资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
