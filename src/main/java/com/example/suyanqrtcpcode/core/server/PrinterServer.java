package com.example.suyanqrtcpcode.core.server;

import com.example.suyanqrtcpcode.core.decoder.HeartbeatDecoder;
import com.example.suyanqrtcpcode.core.handle.PrinterHandler;
import com.example.suyanqrtcpcode.core.handle.WebSocketServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;

@Component
public class PrinterServer {
    private final static Logger LOGGER = LoggerFactory.getLogger(PrinterServer.class);

    private EventLoopGroup boss = new NioEventLoopGroup();
    private EventLoopGroup work = new NioEventLoopGroup();


    private EventLoopGroup bossWs = new NioEventLoopGroup();
    private EventLoopGroup workWs = new NioEventLoopGroup();



    /**
     * 启动 Netty
     *  初始化操作
     * @return
     * @throws InterruptedException
     */
    @PostConstruct
    public void start() throws InterruptedException {

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(boss, work)
                .channel(NioServerSocketChannel.class)
                .localAddress(new InetSocketAddress(8888))
                //保持长连接
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new PrinterInitializer());

            ChannelFuture future = bootstrap.bind().sync();
            if (future.isSuccess()) {
                LOGGER.info("启动 Netty 成功");
            }


        //创建一个webScoket的netty内容
        ServerBootstrap bootstrapWs = new ServerBootstrap()
                .group(bossWs, workWs)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new WebsocketChannelInitializer());
        ChannelFuture futureWs = bootstrapWs.bind(9999).sync();
        if (futureWs.isSuccess()) {
            LOGGER.info("启动 NettyWS 成功");
        }
    }

    public class PrinterInitializer extends   ChannelInitializer<Channel>{
        protected void initChannel(Channel ch) throws Exception {
            ch.pipeline()
                    //五秒没有收到消息 将IdleStateHandler 添加到 ChannelPipeline 中
                    .addLast(new IdleStateHandler(40, 0, 0))
                    .addLast(new StringEncoder())
                    .addLast(new HeartbeatDecoder())
                    .addLast(new PrinterHandler());
        }
    }


    public class WebsocketChannelInitializer extends ChannelInitializer<SocketChannel> {

        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {

            //获取pipeline通道
            ChannelPipeline pipeline = socketChannel.pipeline();
            //因为基于http协议，使用http的编码和解码器
            pipeline.addLast(new HttpServerCodec());
            //是以块方式写，添加ChunkedWriteHandler处理器
            pipeline.addLast(new ChunkedWriteHandler());
        /*
          说明
          1. http数据在传输过程中是分段, HttpObjectAggregator ，就是可以将多个段聚合
          2. 这就就是为什么，当浏览器发送大量数据时，就会发出多次http请求
        */
            pipeline.addLast(new HttpObjectAggregator(65536));
        /* 说明
          1. 对应websocket ，它的数据是以 帧(frame) 形式传递
          2. 可以看到WebSocketFrame 下面有六个子类
          3. 浏览器请求时 ws://localhost:7000/msg 表示请求的uri
          4. WebSocketServerProtocolHandler 核心功能是将 http协议升级为 ws协议 , 保持长连接
          5. 是通过一个 状态码 101
        */
            pipeline.addLast(new WebSocketServerProtocolHandler("/deviceDataSck"));
            //自定义的handler ，处理业务逻辑
            pipeline.addLast(new WebSocketServerHandler());
        }
    }


    /**
     * 销毁
     */
    @PreDestroy
    public void destroy() {
        boss.shutdownGracefully().syncUninterruptibly();
        work.shutdownGracefully().syncUninterruptibly();
        bossWs.shutdownGracefully().syncUninterruptibly();
        workWs.shutdownGracefully().syncUninterruptibly();
        LOGGER.info("关闭 Netty 成功");
    }

}
