package com.example.suyanqrtcpcode;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAdminServer
@Slf4j
public class SuyanQrtcpcodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SuyanQrtcpcodeApplication.class, args);
    }

}
