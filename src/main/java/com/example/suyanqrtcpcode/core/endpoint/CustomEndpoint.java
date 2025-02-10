package com.example.suyanqrtcpcode.core.endpoint;

import com.example.suyanqrtcpcode.utils.NettySocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Endpoint(id = "customendpoint")
public class CustomEndpoint {

    @ReadOperation
    public Map<String, NioSocketChannel> customEndpoint() {
        return NettySocketHolder.getMAP();
    }

}
