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
        if(!taskEnabled){
            log.info("定时器配置未打开！");
            return;
        }

        long listLength = redisUtils.getListLength(HttpUrlFactory.QRCODEMANAGEMENTGENERATEBUCKET);
        //通知页面没有数据进行上传了
        JSONObject uploadJo=new JSONObject();
        uploadJo.put(WsConstant.msgtype.PRINTER_UPLOAD_STATUS,listLength);
        WebSocketServerHandler.sendAll(uploadJo.toString());
        if(listLength!=0){
            // 定时的去推送消息内容，这里可以考虑使用队列而不是http消息接口
            String firstElement = redisUtils.getFirstElement(HttpUrlFactory.QRCODEMANAGEMENTGENERATEBUCKET);
            String httpUrl = HttpUrlFactory.getHttpUrl("createQrCodeBucket");
            JSONObject jo = JSONObject.parseObject(firstElement);
            // 调用post方法去执行内容
            MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
            paramMap.add("bizBucketTypeId", jo.getString("bizBucketTypeld"));// 设备号，
            paramMap.add("terminalSign", jo.getString("terminalSign"));// 账号密码
            paramMap.add("qrCode", jo.getString("qrCode"));// 账号密码
            paramMap.add("qno", jo.getString("qno"));// 账号密码
            paramMap.add("userName", "shuitongchang");
            paramMap.add("userPwd","123456");
            try {
                RestTemplateUtil restTemplateUtil = new RestTemplateUtil(restTemplatel);
                JSONObject resJo = restTemplateUtil.postRestTemplate(httpUrl, paramMap);
                if (resJo == null) {
                    log.error("返回内容为空>>>>>>>>>>>" + jo.getString("qrCode") + "未绑定成功" + jo.getString("qno"));
                    sendEmail(jo,"返回内容为空>>>>>>>>>>>");
                    return;
                }
                int intValue = resJo.getIntValue("state");
                if (intValue == 1) {
                    // 删掉redis存在的内容信息.性能够用不需要使用其他的持久化策略
//                    redisUtils.removeElement(HttpUrlFactory.QRCODEMANAGEMENTGENERATEBUCKET);
//                    redisUtils.incr(HttpUrlFactory.QRCODEMANAGEMENTCOUNTERKEY);
                    redisUtils.addLua();
                } else {
                    //这里要使用lua脚本
                    String element  = redisUtils.removeElement(HttpUrlFactory.QRCODEMANAGEMENTGENERATEBUCKET);
                    //放入新的队列中去
                    redisUtils.pushBack(HttpUrlFactory.QRCODEMANAGEMENTERRORBUCKET,element);
                    String message = resJo.getString("message");
                    log.error("返回信息不成功>>>>>>>>>>>" + jo.getString("qrCode") + "未绑定成功" + jo.getString("qno") + "错误信息" + message);
                    sendEmail(jo,message);
                }
            } catch (Exception e) {
                log.error("异常信息>>>>>>>>>>>" + jo.getString("qrCode") + "未绑定成功" + jo.getString("qno"),e);
                sendEmail(jo,e.getMessage());
            }
        }

    }

    private void sendEmail(JSONObject jo,String message) {
        String  str= jo.getString("qrCode") + "未绑定成功" + jo.getString("qno") + "错误信息" + message;
        // ===============发送邮件================
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = currentDateTime.format(formatter);
        MailUtil.send("1519583238@qq.com", "生产情况"+formattedDateTime, str, false);
        // ===============发送邮件================
    }


    /**
     * 错误的重试机制,把当前的数据往后面移动
     */
    public  void  retryBingDing(){
        
    }

}
