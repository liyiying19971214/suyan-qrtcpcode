package com.example.suyanqrtcpcode.schedule;

import com.example.suyanqrtcpcode.controller.PrinterController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;



@Slf4j
@Component
public class LoadingQrCodeExecutor {
    

    @Autowired
    private PrinterController printerController;

    @Async    
    public void myMethod() {
          // 执行任务的代码
            try {
                printerController.saveRedisCode();
            } catch (Exception e) {
                log.error("线程中加载编号和二维码失败！！！", e);
            }
  
      }

}
