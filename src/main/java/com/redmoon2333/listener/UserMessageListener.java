package com.redmoon2333.listener;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserMessageListener {

    @RabbitListener(queues = "user.registered.queue")
    public void handleUserRegistered(@Payload Map<String, Object> userInfo,
            Channel channel,
            long deliveryTag) {
        log.info("收到用户注册消息：{}", userInfo);
        try {
            // TODO: 这里可以添加用户注册后的处理逻辑
            // 例如：发送欢迎邮件、发放新人优惠券、初始化用户配置等
            channel.basicAck(deliveryTag, false);
            log.info("用户注册消息处理成功");
        } catch (Exception e) {
            log.error("处理用户注册消息失败：{}", e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("拒绝消息失败：{}", ex.getMessage(), ex);
            }
        }
    }
}
