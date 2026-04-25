package com.redmoon2333.mq.consumer;

import com.redmoon2333.dto.NotifyEvent;
import com.redmoon2333.entity.Message;
import com.redmoon2333.mapper.MessageMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class InAppMsgConsumer {

    private final MessageMapper messageMapper;

    @RabbitListener(queues = "notify.inapp.queue")
    public void consume(NotifyEvent event, Channel channel, org.springframework.amqp.core.Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            Message msg = new Message();
            msg.setReceiverId(event.getReceiverId());
            msg.setSenderId(event.getSenderId());
            msg.setType(event.getType().name());
            msg.setTitle(event.getTitle());
            msg.setContent(event.getContent());
            msg.setRefType(event.getRefType());
            msg.setRefId(event.getRefId());
            msg.setIsRead(false);
            messageMapper.insert(msg);
            channel.basicAck(deliveryTag, false);
            log.info("站内信写入成功: eventId={}, receiverId={}", event.getEventId(), event.getReceiverId());
        } catch (Exception e) {
            log.error("站内信写入失败: eventId={}, error={}", event.getEventId(), e.getMessage());
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
