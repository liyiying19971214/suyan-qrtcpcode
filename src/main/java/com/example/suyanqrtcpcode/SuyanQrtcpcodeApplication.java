package com.example.suyanqrtcpcode;

import com.alibaba.fastjson.JSONObject;
import de.codecentric.boot.admin.server.config.EnableAdminServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAdminServer
@EnableScheduling
public class SuyanQrtcpcodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SuyanQrtcpcodeApplication.class, args);
    }


}
