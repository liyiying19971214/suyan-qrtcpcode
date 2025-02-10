package com.example.suyanqrtcpcode.schedule;

import com.example.suyanqrtcpcode.runner.DataFileRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;




@Configuration      
@EnableScheduling   
public class DataFileSchedule {
	
	@Autowired
	private DataFileRunner dfr;


	
    @Scheduled(cron = "0 0 0 * * ?")
    //或直接指定时间间隔，例如：5秒
    //@Scheduled(fixedRate=5000)
    private void configureTasks() {
    	try {
			dfr.run(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
