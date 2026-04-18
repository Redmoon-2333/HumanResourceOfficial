package com.redmoon2333.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息队列监控接口
 */
@RestController
@RequestMapping("/api/mq")
@RequiredArgsConstructor
public class MQMonitorController {

    private final ConnectionFactory connectionFactory;

    /**
     * 检查 MQ 连接状态
     */
    @GetMapping("/health")
    public Map<String, Object> checkHealth() {
        Map<String, Object> result = new HashMap<>();
        try {
            connectionFactory.createConnection();
            result.put("status", "UP");
            result.put("message", "RabbitMQ 连接正常");
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("message", "RabbitMQ 连接失败：" + e.getMessage());
        }
        return result;
    }
}
