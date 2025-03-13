package com.example.suyanqrtcpcode.schedule;

import com.example.suyanqrtcpcode.service.RedisCodeService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class LoadingQrCodeExecutor {

    @Setter
    private RedisCodeService redisCodeService;

    /**
     * 异步调用方法内容
     */
    @Async    
    public void myMethod(String  factoryId) {
          // 执行任务的代码
            try {
                redisCodeService.saveRedisCode(factoryId);
            } catch (Exception e) {
                log.error("线程中加载编号和二维码失败！！！", e);
            }
  
      }

}
