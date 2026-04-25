package com.redmoon2333.mq.consumer;

import com.redmoon2333.dto.NotifyEvent;
import com.redmoon2333.service.EmailService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = "notify.email.queue")
    public void consume(NotifyEvent event, Channel channel, Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        log.info(">>> [邮件消费者] 收到消息: deliveryTag={}, eventId={}, type={}, receiverId={}", deliveryTag, event.getEventId(), event.getType(), event.getReceiverId());
        try {
            log.info(">>> [邮件消费者] 调用 EmailService 发送邮件...");
            emailService.sendNotificationEmail(event);
            channel.basicAck(deliveryTag, false);
            log.info(">>> [邮件消费者] 邮件发送成功，已 ACK: eventId={}, receiverId={}", event.getEventId(), event.getReceiverId());
        } catch (Exception e) {
            log.error(">>> [邮件消费者] 邮件发送失败，已 NACK: eventId={}, error={}", event.getEventId(), e.getMessage(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
