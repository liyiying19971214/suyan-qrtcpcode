package com.example.suyanqrtcpcode.core.handle;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.suyanqrtcpcode.bean.CustomProtocol;
import com.example.suyanqrtcpcode.bean.PrinterConfigData;
import com.example.suyanqrtcpcode.constants.HttpUrlFactory;
import com.example.suyanqrtcpcode.constants.QRConstant;
import com.example.suyanqrtcpcode.constants.WsConstant;
import com.example.suyanqrtcpcode.core.service.StorageService;
import com.example.suyanqrtcpcode.exceptions.BusinessException;
import com.example.suyanqrtcpcode.runner.LoadDataRunner;
import com.example.suyanqrtcpcode.schedule.LoadingQrCodeExecutor;
import com.example.suyanqrtcpcode.utils.NettySocketHolder;
import com.example.suyanqrtcpcode.utils.PropertiesUtil;
import com.example.suyanqrtcpcode.utils.RedisUtils;
import com.example.suyanqrtcpcode.utils.SpringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 这里不能使用spring注入容器因为是new出来的内容
 */
public class PrinterHandler extends SimpleChannelInboundHandler<CustomProtocol> {

    private final static String sync="sync";
    private static boolean ready=false;

    private static int maxId = 0; // 记录当前最大ID值

    private static ChannelHandlerContext ctx=null;

    private static RedisUtils redisUtils;

    private static LoadingQrCodeExecutor loadingQrCodeExecutor;

    private  static  StorageService storageSvc;

    private static int  surListLength;



    @Value("${taskEnabled}")
    private  Boolean  taskEnabled;



    /**
     * 犹豫这个是new出来的无法使用放入spring进行管理
     */
    static {
        redisUtils = SpringUtil.getBean(RedisUtils.class);
        loadingQrCodeExecutor = SpringUtil.getBean(LoadingQrCodeExecutor.class);
        storageSvc = SpringUtil.getBean(StorageService.class);

        PropertiesUtil pu=new PropertiesUtil("suyan-config.properties");
        surListLength=Integer.parseInt(pu.readProperty("redis.surListLength"));

    }



    private final static Logger LOGGER = LoggerFactory.getLogger(PrinterHandler.class);
    private static final ByteBuf HEART_BEAT =  Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(new CustomProtocol("locahost","kong").toString(), CharsetUtil.UTF_8));

    public static boolean isConnected() {
        return  ctx!=null;
    }

    public static void clearPrintedContent() {
        PrinterConfigData.setQrNumber(null);
        PrinterConfigData.setQrCode(null);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CustomProtocol customProtocol) throws Exception {
        LOGGER.info("收到customProtocol={}", customProtocol);
        //保存客户端与 Channel 之间的关系
        NettySocketHolder.put(customProtocol.getId(),(NioSocketChannel)ctx.channel()) ;
        //这里是需要进行获取内容的
        // super.messageReceived(session, message);
        String  message=customProtocol.getContent();
        LOGGER.info("printer has recived ---------:"+message.toString());
        if(message!=null && (("string".equals(message.toString().trim())  && PrinterConfigData.getQrCode()!=null  && PrinterConfigData.getQrNumber()!=null )
                ||  ("givecode".equals(message.toString().trim()) &&  PrinterConfigData.getQrNumber()==null  ))
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
                        if ("string".equals(message.toString().trim())) {
                            jo.put(WsConstant.msgtype.GENERATE_BUCKET_MSG, "等待绑定水桶,请稍等~~~~");
                            //内容赋值
                            msg=PrinterConfigData.getQrCode();
                        } else if ("givecode".equals(message.toString().trim())) {
                            //code = getQrCode("getQrCodeIndex");
                            //检查数量原子性操作
                            long listLength = redisUtils.getListLength(HttpUrlFactory.QRCODEMANAGEMENTKEYVALUS);
                            if(listLength<=surListLength){
                                //开启新线程去执行
                                loadingQrCodeExecutor.myMethod();
                            }
                            //弹出内容信息
                            String firstElement= redisUtils.removeElement(HttpUrlFactory.QRCODEMANAGEMENTKEYVALUS);

                            if(StringUtils.isEmpty(firstElement)){
                               throw new BusinessException("电脑二维码内容已用完,重试");
                            }
                            JSONObject firstObj = JSONObject.parseObject(firstElement);
                            String index=firstObj.getString("index");
                            String  code=firstObj.getString("code");
                            PrinterConfigData.setQrNumber(index);
                            PrinterConfigData.setQrCode(HttpUrlFactory.DOMAIN_NAME+code);
                            jo.put(WsConstant.msgtype.GENERATE_BUCKET_FLAG, false);
                            //内容赋值
                            msg=PrinterConfigData.getQrNumber();
                        }
                        PrinterHandler.sendMyMsg(msg);
                        //ctx.writeAndFlush("22222");
                        //WebSocketServerHandler.sendAll(jo.toString());
                        LOGGER.debug("========"+msg);
                        //这里是进行水桶绑定情况
                        if (PrinterConfigData.getQrCode() != null && PrinterConfigData.getQrNumber() != null
                                && "string".equals(message.toString().trim())) {
                            JSONObject sbJo = saveBucket();
                            WebSocketServerHandler.sendAll(sbJo.toString());
                        }
                    } catch (Exception e) {
                        LOGGER.error("错误信息",e);
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
                LOGGER.info("已经20秒没有收到信息！");

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
        LOGGER.info("客户端连接进来"+ctx.channel().remoteAddress());
        this.ctx=ctx;
        JSONObject jo=new JSONObject();
        jo.put(WsConstant.msgtype.PRINTER_CONNECTION_STATUS.toString(),true);
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
    public JSONObject  saveBucket() {
        //删除数据内容
        JSONObject  resJo=new JSONObject();
        resJo.put(WsConstant.msgtype.GENERATE_BUCKET_FLAG, true);
        //插入excel
        try {
            JSONObject msgJo=new  JSONObject();
            msgJo.put("bucketId", PrinterConfigData.getQrNumber());
            msgJo.put("QRCodeStr", PrinterConfigData.getQrCode());
            storageSvc.save2MyLog(msgJo);
            resJo.put(WsConstant.msgtype.PRINT_CNT, LoadDataRunner.getDataMapCnt());
        } catch (Exception e) {
            e.printStackTrace();
        }
        //存储数据
        Map<String, String> map=new HashMap<>();
        map.put("terminalSign", "358406100096392");
        map.put("bizBucketTypeld", PrinterConfigData.getBucketTypeId());
        map.put("qno", PrinterConfigData.getQrNumber());
        map.put("qrCode", PrinterConfigData.getQrCode());
        map.put("userName", "laoshanadmin");
        map.put("userPwd", "123456");
        redisUtils.pushBack(HttpUrlFactory.QRCODEMANAGEMENTGENERATEBUCKET, JSON.toJSONString(map));
        //存储最后一次的num
        PrinterConfigData.setLastqrNumber(PrinterConfigData.getQrNumber());
        PrinterConfigData.setQrNumber(null);
        PrinterConfigData.setQrCode(null);
        resJo.put(WsConstant.msgtype.GENERATE_BUCKET_MSG, "水桶绑定成功");
        return resJo;
//	        JSONObject  resJo=new JSONObject();
//	        String url = HttpUrlFactory.getHttpUrl("createQrCodeBucket");
//	        MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
//	        paramMap.add("bizBucketTypeId", PrinterConfigData.getBucketTypeId());// 设备号，
//	        paramMap.add("terminalSign", "990008450019737");// 账号密码
//	        paramMap.add("qrCode", PrinterConfigData.getQrCode());// 账号密码
//	        paramMap.add("qno", PrinterConfigData.getQrNumber());// 账号密码
//	        paramMap.add("userName", "laoshanadmin");
//	        paramMap.add("userPwd", "123456");
//	        JSONObject jo = RestTemplateUtil.postRestTemplate(url, paramMap);
//	        if (jo!= null) {
//                Integer state = jo.getInteger("state");//这里注意超过127救出出错
//                String msg = jo.getString("message");
//                if (state == 1) {
//                    String bId = jo.getString("bId");
//                    PrinterConfigData.setbId(bId);//存入数据
//                    resJo.put(WsConstant.msgtype.GENERATE_BUCKET_FLAG, true);
//
//                    //插入excel
//                    try {
//                        JSONObject msgJo=new  JSONObject();
//                        msgJo.put("bucketId", PrinterConfigData.getQrNumber());
//                        msgJo.put("QRCodeStr", PrinterConfigData.getQrCode());
//                        storageSvc.save2MyLog(msgJo);
//                        resJo.put(WsConstant.msgtype.PRINT_CNT, LoadDataRunner.getDataMapCnt());
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//
//                    PrinterConfigData.setQrNumber(null);
//                    PrinterConfigData.setQrCode(null);
//                }
//                resJo.put(WsConstant.msgtype.GENERATE_BUCKET_MSG, msg);
//            }
//	        return resJo;
    }


    private static void sendMessage(ChannelHandlerContext ctx, Object message){
        if(ctx == null){
            return;
        }
        ctx.writeAndFlush(message);
    }
}

