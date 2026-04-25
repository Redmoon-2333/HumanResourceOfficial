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
public class ActivityMessageListener {

    @RabbitListener(queues = "activity.created.queue")
    public void handleActivityCreated(@Payload Map<String, Object> activity,
            Channel channel,
            long deliveryTag) {
        log.info("收到活动创建消息：{}", activity);
        try {
            // TODO: 这里可以添加活动创建后的处理逻辑
            // 例如：发送通知、更新缓存、触发 AI 学习等
            channel.basicAck(deliveryTag, false);
            log.info("活动创建消息处理成功");
        } catch (Exception e) {
            log.error("处理活动创建消息失败：{}", e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("拒绝消息失败：{}", ex.getMessage(), ex);
            }
        }
    }
}
