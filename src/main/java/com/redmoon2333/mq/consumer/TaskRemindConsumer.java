package com.redmoon2333.mq.consumer;

import com.redmoon2333.dto.NotifyEvent;
import com.redmoon2333.dto.NotifyType;
import com.redmoon2333.mq.producer.NotifyProducer;
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
public class TaskRemindConsumer {

    private final NotifyProducer notifyProducer;

    @RabbitListener(queues = "task.remind.queue")
    public void consume(NotifyEvent event, Channel channel, Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            notifyProducer.send(NotifyEvent.builder()
                    .type(NotifyType.TASK_REMIND)
                    .receiverId(event.getReceiverId())
                    .senderId(event.getSenderId())
                    .title(event.getTitle())
                    .content(event.getContent())
                    .refType(event.getRefType())
                    .refId(event.getRefId())
                    .build());
            channel.basicAck(deliveryTag, false);
            log.info("催促延时消息转发成功: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("催促延时消息转发失败: eventId={}, error={}", event.getEventId(), e.getMessage());
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
