package com.example.suyanqrtcpcode.core.handle;


import com.alibaba.fastjson.JSONObject;
import com.example.suyanqrtcpcode.bean.PrinterConfigData;
import com.example.suyanqrtcpcode.constants.HttpUrlFactory;
import com.example.suyanqrtcpcode.constants.WsConstant;
import com.example.suyanqrtcpcode.controller.PrinterController;
import com.example.suyanqrtcpcode.utils.RedisUtils;
import com.example.suyanqrtcpcode.utils.SpringUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class WebSocketServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private final static Logger LOGGER = LoggerFactory.getLogger(WebSocketServerHandler.class);

    /**
     * 用来存放内容，好统一发送信息数据
     */
    private static List<ChannelHandlerContext> clients = new ArrayList<>();


    private static RedisUtils redisUtils;


    static {
        redisUtils = SpringUtil.getBean(RedisUtils.class);
    }

    /**
     * 发送消息内容
     * @param
     */
    public static void sendAll(String msg) {
        for (ChannelHandlerContext client : clients) {
            client.channel().writeAndFlush(new TextWebSocketFrame(msg));
        }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        // 处理消息
        LOGGER.info("Received message: " + msg.text());

        JSONObject jsonObject = JSONObject.parseObject(msg.text());
        String msgtype = jsonObject.getString("msgtype");
        if(WsConstant.msgtype.SET_SCRAP_BUCKET.equals(msgtype)){
            scrapBucket();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 添加连接
        LOGGER.info("Client connected: " + ctx.channel());
        clients.add(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 断开连接
        LOGGER.info("Client disconnected: " + ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 异常处理
        cause.printStackTrace();
        ctx.close();
    }


    /**
     * 水桶报废接口
     * @return
     */
    public    void  scrapBucket() {
        //把当前的数据内容存入到里面
        JSONObject  resJo=new JSONObject();
        try{
            redisUtils.pushBack(HttpUrlFactory.QRCODEMANAGEMENTDESTRUCTIONKEY, PrinterConfigData.getLastqrNumber());
            //统一手动处理
            resJo.put(WsConstant.msgtype.GENERATE_BUCKET_FLAG, false);
            resJo.put(WsConstant.msgtype.GENERATE_BUCKET_MSG, " 水桶报废处理成功~~~)");
            //增加一个报废弹框
            resJo.put(WsConstant.msgtype.DESTRUCTION_ALERT_MSG, true);
        }catch(Exception e){
            //统一手动处理
            resJo.put(WsConstant.msgtype.GENERATE_BUCKET_FLAG, false);
            resJo.put(WsConstant.msgtype.GENERATE_BUCKET_MSG, "报废异常联系管理员");
            //增加一个报废弹框
            resJo.put(WsConstant.msgtype.DESTRUCTION_ALERT_MSG, false);
        }finally {
            WebSocketServerHandler.sendAll(resJo.toString());
        }
    }

}