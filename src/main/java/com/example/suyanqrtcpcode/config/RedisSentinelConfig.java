package com.example.suyanqrtcpcode.config;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.models.role.RedisInstance;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.NamedNode;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.util.Set;

public class RedisSentinelConfig {
    @Value("${spring.redis.sentinel.nodes}")
    private Set<String> nodes;
    @Value("${spring.redis.sentinel.master}")
    private String master;

    @Value("${spring.redis.lettuce.pool.max-idle}")
    private int maxIdle;
    @Value("${spring.redis.lettuce.pool.min-idle}")
    private int minIdle;
    @Value("${spring.redis.lettuce.pool.max-wait}")
    private long maxWait;
    @Value("${spring.redis.lettuce.pool.max-active}")
    private int maxActive;


    @Bean
    public RedisConnectionFactory lettuceConnectionFactory() {
        RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration(master, nodes);
        NamedNode master = redisSentinelConfiguration.getMaster();
        String name = master.getName();
        //redisSentinelConfiguration.setPassword(RedisPassword.of(password.toCharArray()));
        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        genericObjectPoolConfig.setMaxIdle(maxIdle);
        genericObjectPoolConfig.setMinIdle(minIdle);
        genericObjectPoolConfig.setMaxTotal(maxActive);
        genericObjectPoolConfig.setMaxWaitMillis(maxWait);
        //readFrom(ReadFrom.REPLICA) 可设置，设置了就形成读写分离，读会读取从节点，但是因为有复制过程，要能容忍短时间的脏数据，适合对数据要求不太及时的
        LettucePoolingClientConfiguration lettuceClientConfiguration = LettucePoolingClientConfiguration.builder()
                .poolConfig(genericObjectPoolConfig).readFrom(ReadFrom.REPLICA)
                .build();

        return new LettuceConnectionFactory(redisSentinelConfiguration, lettuceClientConfiguration);
    }


    public void setNodes(Set<String> nodes) {
        this.nodes = nodes;
    }

    public void setMaster(String master) {
        this.master = master;
    }

}