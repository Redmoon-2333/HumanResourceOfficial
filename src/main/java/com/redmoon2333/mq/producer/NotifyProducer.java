package com.redmoon2333.mq.producer;

import com.redmoon2333.dto.NotifyEvent;
import com.redmoon2333.util.MQSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyProducer {

    private final MQSender mqSender;

    public void send(NotifyEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getEventTime() == null) {
            event.setEventTime(LocalDateTime.now());
        }
        log.info(">>> [Notify Producer] 发送通知事件: eventId={}, type={}, receiverId={}, title={}", event.getEventId(), event.getType(), event.getReceiverId(), event.getTitle());
        mqSender.send("notify.fanout", "", event);
        log.info(">>> [Notify Producer] 通知事件已投递到 notify.fanout 交换机: eventId={}", event.getEventId());
    }

    public void sendDelayed(NotifyEvent event, long delayMs) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getEventTime() == null) {
            event.setEventTime(LocalDateTime.now());
        }
        log.info("发送延时通知事件: eventId={}, type={}, delay={}ms", event.getEventId(), event.getType(), delayMs);
        mqSender.sendDelayed("task.remind.ttl.exchange", "task.remind", event, delayMs);
    }
}
