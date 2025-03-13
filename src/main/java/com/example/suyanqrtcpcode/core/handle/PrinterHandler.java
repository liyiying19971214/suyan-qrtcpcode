package com.example.suyanqrtcpcode.core.handle;

import com.alibaba.fastjson.JSONObject;
import com.example.suyanqrtcpcode.bean.CustomProtocol;
import com.example.suyanqrtcpcode.bean.PrinterConfigData;
import com.example.suyanqrtcpcode.constants.HttpUrlFactory;
import com.example.suyanqrtcpcode.constants.QRConstant;
import com.example.suyanqrtcpcode.constants.WsConstant;
import com.example.suyanqrtcpcode.service.RedisCodeService;
import com.example.suyanqrtcpcode.utils.NettySocketHolder;
import com.example.suyanqrtcpcode.utils.SpringUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * 这里不能使用spring注入容器因为是new出来的内容
 */

@Slf4j
public class PrinterHandler extends SimpleChannelInboundHandler<CustomProtocol> {

    private final static String sync="sync";
    private static boolean ready=false;
    private static ChannelHandlerContext ctx=null;
    private  static final RedisCodeService redisCodeService;
    static {
          redisCodeService = SpringUtil.getBean(RedisCodeService.class);
    }


    @Value("${taskEnabled}")
    private  Boolean  taskEnabled;

    public static boolean isConnected() {
        return  ctx!=null;
    }

    public static void clearPrintedContent() {
        PrinterConfigData.setQrNumber(null);
        PrinterConfigData.setQrCode(null);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CustomProtocol customProtocol) throws Exception {
        log.info("收到customProtocol={}", customProtocol);
        //保存客户端与 Channel 之间的关系
        NettySocketHolder.put(customProtocol.getId(),(NioSocketChannel)ctx.channel()) ;
        //这里是需要进行获取内容的
        // super.messageReceived(session, message);
        String  message=customProtocol.getContent();
        log.info("printer has recived ---------:"+ message);
        if(message!=null && (("string".equals(message.trim())  && PrinterConfigData.getQrCode()!=null  && PrinterConfigData.getQrNumber()!=null )
                ||  ("givecode".equals(message.trim()) &&  PrinterConfigData.getQrNumber()==null  ))
        ){
            synchronized (sync) {
                ready = true;
                String bucketMode = PrinterConfigData.getBucketMode();
                if (bucketMode == null) {
                    JSONObject jo = new JSONObject();
                    jo.put(WsConstant.msgtype.SERVER_SEND_MSG, "打印信息未进行配置");// 传递的消息
                    WebSocketServerHandler.sendAll(jo.toString());
                    return;
                }
                if (WsConstant.constant.PRINTER_CONFIG_BUCKETMODE.equals(bucketMode)) {
                    // 发送信息给手持机
                    CellPhoneHandler.sendMyMsg(QRConstant.StatusCode.PIRINTER_IS_READY);
                    JSONObject jo = new JSONObject();
                    jo.put(WsConstant.msgtype.CELLPHONE_SEND_MSG, "");
                    jo.put(WsConstant.msgtype.PRINTER_RECEIVE_MSG, "");
                    jo.put(WsConstant.msgtype.PRINTER_READY_FLAG, true);
                    WebSocketServerHandler.sendAll(jo.toString());
                } else {
                    // 直接调用获取qrcode
                    JSONObject rejo = new JSONObject();
                    // 提醒打印机就绪的状态
                    rejo.put(WsConstant.msgtype.PRINTER_READY_FLAG, true);
                    WebSocketServerHandler.sendAll(rejo.toString());
                    JSONObject jo = new JSONObject();
                    try {
                        String msg="";
                        if ("string".equals(message.trim())) {
                            jo.put(WsConstant.msgtype.GENERATE_BUCKET_MSG, "等待绑定水桶,请稍等~~~~");
                            //内容赋值
                            msg=PrinterConfigData.getQrCode();
                        } else if ("givecode".equals(message.trim())) {
                            //code = getQrCode("getQrCodeIndex");
                            //弹出内容信息.这里考虑lua脚本来编写防止出现丢失数据
                            String firstElement = redisCodeService.removeFirstCode(PrinterConfigData.getFactoryId());
                            JSONObject firstObj = JSONObject.parseObject(firstElement);
                            String  index=firstObj.getString("index");
                            String  code=firstObj.getString("code");
                            PrinterConfigData.setQrNumber(index);
                            //TODO 多线程下从副本对象中去取内容，这里是单线程
                            PrinterConfigData.setQrCode(PrinterConfigData.getDomainName()+code);
                            jo.put(WsConstant.msgtype.GENERATE_BUCKET_FLAG, false);
                            //内容赋值
                            msg=PrinterConfigData.getQrNumber();
                        }
                        PrinterHandler.sendMyMsg(msg);
                        log.debug("========"+msg);
                        //这里是进行水桶绑定情况
                        if (PrinterConfigData.getQrCode() != null && PrinterConfigData.getQrNumber() != null
                                && "string".equals(message.trim())) {
                            JSONObject sbJo = saveBucket();
                            WebSocketServerHandler.sendAll(sbJo.toString());
                        }
                    } catch (Exception e) {
                        log.error("错误信息",e);
                        jo.put(WsConstant.msgtype.SERVER_SEND_MSG, "错误信息:" + e.getMessage());
                        jo.put(WsConstant.msgtype.CELLPHONE_SEND_MSG, message);
                        WebSocketServerHandler.sendAll(jo.toString());
                    }

                }

            }
        }


    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof IdleStateEvent){
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt ;
            if (idleStateEvent.state() == IdleState.READER_IDLE){
                log.info("已经20秒没有收到信息！");

               // ctx.writeAndFlush(HEART_BEAT).addListener(ChannelFutureListener.CLOSE_ON_FAILURE) ;
            }

        }

        super.userEventTriggered(ctx, evt);
    }


    /**
     * 客户端连接事件内容
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端连接进来"+ctx.channel().remoteAddress());
        PrinterHandler.ctx =ctx;
        JSONObject jo=new JSONObject();
        jo.put(WsConstant.msgtype.PRINTER_CONNECTION_STATUS,true);
        jo.put(WsConstant.msgtype.PRINTER_CONNECTED_IP,getConnectedIp());
        WebSocketServerHandler.sendAll(jo.toString());
    }

    /**
     * 取消绑定
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端断开连接"+ctx.channel().remoteAddress());
        PrinterHandler.ctx =ctx;
        JSONObject jo=new JSONObject();
        jo.put(WsConstant.msgtype.PRINTER_CONNECTION_STATUS,false);
        jo.put(WsConstant.msgtype.PRINTER_CONNECTED_IP,getConnectedIp());
        WebSocketServerHandler.sendAll(jo.toString());
        NettySocketHolder.remove((NioSocketChannel) ctx.channel());
    }


    public static void setPrinterNotReady(){
        synchronized (sync) {
            ready=false;
            JSONObject jo=new JSONObject();
            jo.put(WsConstant.msgtype.PRINTER_READY_FLAG, false);
            WebSocketServerHandler.sendAll(jo.toString());
        }
    }


    public static String getConnectedIp(){
        if(ctx!=null){
            return ctx.channel().remoteAddress().toString().replace('/', ' ');
        }
        return "";
    }


    public static boolean sendMyMsg(Object message){

        synchronized (sync) {
            boolean flag=false;
            JSONObject jo=new JSONObject();
            jo.put(WsConstant.msgtype.DATA_TRANSFER_FLAG, false);
            if(ctx==null){
                CellPhoneHandler.sendMyMsg(QRConstant.StatusCode.PRINTER_NOT_READY);
            }else if(ready){
                ready=false;
                sendMessage(ctx,message);
                flag=true;
                if(WsConstant.constant.PRINTER_CONFIG_BUCKETMODE.equals(PrinterConfigData.getBucketMode())){
                    jo.put(WsConstant.msgtype.PRINTER_RECEIVE_MSG,message);
                    jo.put(WsConstant.msgtype.PRINTER_READY_FLAG, false);
                }else{
                    jo.put(WsConstant.msgtype.PRINTER_RECEIVE_MSG,PrinterConfigData.getQrNumber());
                }
            }else{
                CellPhoneHandler.sendMyMsg(QRConstant.StatusCode.PRINTER_IS_BUSY);
            }
            WebSocketServerHandler.sendAll(jo.toString());

            return flag;
        }
    }


    /**
     *
     * @return
     */
    public JSONObject  saveBucket() throws Exception{
        //删除数据内容
        JSONObject  resJo=new JSONObject();
        resJo.put(WsConstant.msgtype.GENERATE_BUCKET_FLAG, true);

        //存储数据
        redisCodeService.pushCodeByMap(PrinterConfigData.getFactoryId());
        //情况数据，保留最后一个
        clearPrinterConfig();
        return resJo;
    }

    private void clearPrinterConfig() {
        PrinterConfigData.setLastqrNumber(PrinterConfigData.getQrNumber());
        PrinterConfigData.setQrNumber(null);
        PrinterConfigData.setQrCode(null);
    }

    private static void sendMessage(ChannelHandlerContext ctx, Object message){
        if(ctx == null){
            return;
        }
        ctx.writeAndFlush(message);
    }
}

