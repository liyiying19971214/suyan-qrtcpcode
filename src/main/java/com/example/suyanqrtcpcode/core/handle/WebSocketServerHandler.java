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
        }catch(Exception e){
            //统一手动处理
            resJo.put(WsConstant.msgtype.GENERATE_BUCKET_FLAG, false);
            resJo.put(WsConstant.msgtype.GENERATE_BUCKET_MSG, "报废异常联系管理员");
        }finally {
            WebSocketServerHandler.sendAll(resJo.toString());
        }
        /**
         String url = HttpUrlFactory.getHttpUrl("scrapBucket");
         MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
         paramMap.add("terminalSign", "990008450019737");// 账号密码
         paramMap.add("GpsX", "116.764402");// 账号密码
         paramMap.add("GpsY", "36.580692");// 账号密码
         paramMap.add("Data", Arrays.asList(PrinterConfigData.getbId()).toString());
         paramMap.add("SelectModel", "DISCARD_BUCKET_LABEL");
         paramMap.add("loginName", "laoshanadmin");
         paramMap.add("companyId", "22222222222222222222222222222");
         paramMap.add("orgCode", "0104");
         paramMap.add("roleId", "402888e557217b04015726b83b570003");
         paramMap.add("userId", "4028e5e4880391ba01880399c3b2000a");
         RestTemplateUtil restTemplateUtil = new RestTemplateUtil(restTemplate);
         JSONObject jo = restTemplateUtil.postRestTemplate(url, paramMap);
         if (jo!= null) {
         Integer state = jo.getInteger("state");//这里注意超过127救出出错
         String msg = jo.getString("message");
         if (state == 1) {
         resJo.put(WsConstant.msgtype.GENERATE_BUCKET_FLAG, false);
         msg="水桶报废处理成功~~~";
         }
         resJo.put(WsConstant.msgtype.GENERATE_BUCKET_MSG, msg);
         }else{
         resJo.put(WsConstant.msgtype.GENERATE_BUCKET_MSG, "请联系管理员，水桶报废出错！");
         }
         WebSocketServerHandler.sendAll(resJo.toString());
         **/
    }



}