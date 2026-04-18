package com.redmoon2333.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 分布式锁配置类
 * 支持 redis:// (开发环境) 和 rediss:// (生产环境 SSL) 协议
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${redisson.ssl.enabled:false}")
    private boolean sslEnabled;

    /**
     * 配置 RedissonClient Bean
     * 用于分布式锁、分布式计数器等高级 Redis 功能
     *
     * @return RedissonClient 实例
     */
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 根据是否启用 SSL 选择协议
        String protocol = sslEnabled ? "rediss" : "redis";
        String address = String.format("%s://%s:%d", protocol, redisHost, redisPort);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer()
                    .setAddress(address)
                    .setPassword(redisPassword);
        } else {
            config.useSingleServer()
                    .setAddress(address);
        }
        
        return Redisson.create(config);
    }
}