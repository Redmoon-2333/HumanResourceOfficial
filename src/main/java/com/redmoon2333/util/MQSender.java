package com.redmoon2333.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 消息队列发送器
 * 封装通用的消息发送逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MQSender {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送消息到指定交换机
     *
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息体
     */
    public void send(String exchange, String routingKey, Object message) {
        log.info("发送 MQ 消息：exchange={}, routingKey={}, message={}", exchange, routingKey, message);
        CorrelationData correlationData = new CorrelationData();
        rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
    }

    /**
     * 发送延迟消息 (通过队列 TTL 实现)
     *
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息体
     * @param delayMs 延迟时间 (毫秒)
     */
    public void sendDelayed(String exchange, String routingKey, Object message, long delayMs) {
        log.info("发送延迟 MQ 消息：exchange={}, routingKey={}, delay={}ms, message={}",
                exchange, routingKey, delayMs, message);
        CorrelationData correlationData = new CorrelationData();
        rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
            msg.getMessageProperties().setDelay((int) delayMs);
            return msg;
        }, correlationData);
    }
}
