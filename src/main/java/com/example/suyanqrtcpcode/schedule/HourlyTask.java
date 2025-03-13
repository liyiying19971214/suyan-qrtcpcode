package com.example.suyanqrtcpcode.schedule;

import cn.hutool.extra.mail.MailUtil;
import com.alibaba.fastjson.JSONObject;
import com.example.suyanqrtcpcode.constants.HttpUrlFactory;
import com.example.suyanqrtcpcode.constants.WsConstant;
import com.example.suyanqrtcpcode.core.handle.WebSocketServerHandler;
import com.example.suyanqrtcpcode.utils.RedisUtils;
import com.example.suyanqrtcpcode.utils.RestTemplateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static io.lettuce.core.pubsub.PubSubOutput.Type.message;

@Slf4j
@Component
@PropertySource("classpath:suyan-config.properties")
public class HourlyTask {
    

    @Autowired
    private RedisUtils redisUtils;
    
    @Autowired
    private  RestTemplate restTemplatel;


    @Value("${scheduled.task.cron}")
    private String cronExpression;

    @Value("${taskEnabled}")
    private  Boolean  taskEnabled;

    @Scheduled(cron = "#{@environment['scheduled.task.cron']}") // 每30秒执行
    public void executeTask() {
        log.info("定时器工作！");
        if(!taskEnabled){
            log.info("定时器配置未打开！");
            return;
        }

        long listLength = redisUtils.getListLength(HttpUrlFactory.QRCODEMANAGEMENTGENERATEBUCKET);
        //通知页面没有数据进行上传了
        JSONObject uploadJo=new JSONObject();
        uploadJo.put(WsConstant.msgtype.PRINTER_UPLOAD_STATUS,listLength);
        WebSocketServerHandler.sendAll(uploadJo.toString());

        // 如果列表不为空，处理第一个元素
        Optional.of(listLength)
                .filter(length -> length != 0)
                .ifPresent(length -> processFirstElement());

    }


    private void processFirstElement() {
        // 获取 Redis 列表中的第一个元素
        Optional<String> firstElementOpt = Optional.ofNullable(redisUtils.getFirstElement(HttpUrlFactory.QRCODEMANAGEMENTGENERATEBUCKET));

        firstElementOpt.ifPresent(firstElement -> {
            JSONObject jo = JSONObject.parseObject(firstElement);
            String httpUrl = HttpUrlFactory.getHttpUrl("createQrCodeBucket");

            // 构建请求参数
            MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
            paramMap.add("bizBucketTypeId", jo.getString("bizBucketTypeld"));
            paramMap.add("terminalSign", jo.getString("terminalSign"));
            paramMap.add("qrCode", jo.getString("qrCode"));
            paramMap.add("qno", jo.getString("qno"));
            paramMap.add("userName", "shuitongchang");
            paramMap.add("userPwd", "123456");

            // 发送 HTTP 请求
            try {
                RestTemplateUtil restTemplateUtil = new RestTemplateUtil(restTemplatel);
                JSONObject resJo = restTemplateUtil.postRestTemplate(httpUrl, paramMap);
                if (resJo != null) {
                    handleResponse(resJo, jo); // 处理响应
                } else {
                    log.error("返回内容为空>>>>>>>>>>>" + jo.getString("qrCode") + "未绑定成功" + jo.getString("qno"));
                    sendEmail(jo, "返回内容为空>>>>>>>>>>>");
                }
            } catch (Exception e) {
                log.error("异常信息>>>>>>>>>>>" + jo.getString("qrCode") + "未绑定成功" + jo.getString("qno"), e);
                sendEmail(jo, e.getMessage());
            }
        });
    }

    private void handleResponse(JSONObject resJo, JSONObject jo) throws  Exception{
        Optional.of(resJo.getIntValue("state"))
                .ifPresent(state -> {
                    if (state == 1) {
                        // 成功时执行 Lua 脚本
                        redisUtils.addLua();
                    } else {
                        // 失败时将元素移到错误队列
                        String element = redisUtils.removeElement(HttpUrlFactory.QRCODEMANAGEMENTGENERATEBUCKET);
                        redisUtils.pushBack(HttpUrlFactory.QRCODEMANAGEMENTERRORBUCKET, element);

                        String message = resJo.getString("message");
                        log.error("返回信息不成功>>>>>>>>>>>" + jo.getString("qrCode") + "未绑定成功" + jo.getString("qno") + "错误信息" + message);
                        sendEmail(jo, message);
                    }
                });
    }


    private void sendEmail(JSONObject jo,String message) {
        try {
            String  str= jo.getString("qrCode") + "未绑定成功" + jo.getString("qno") + "错误信息" + message;
            // ===============发送邮件================
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = currentDateTime.format(formatter);
            MailUtil.send("1519583238@qq.com", "生产情况"+formattedDateTime, str, false);
        }catch (Exception e){
            log.error("邮件功能异常！",e);
        }
        // ===============发送邮件================
    }


    /**
     * 错误的重试机制,把当前的数据往后面移动
     */
    public  void  retryBingDing(){
        
    }

}
