package com.redmoon2333.listener;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 活动消息监听器
 * 监听活动相关事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityMessageListener {

    /**
     * 监听活动创建事件
     *
     * @param activity 活动信息
     * @param channel RabbitMQ channel
     * @param deliveryTag 消息标签
     */
    @RabbitListener(queues = "activity.created.queue")
    public void handleActivityCreated(@Payload Object activity,
            Channel channel,
            long deliveryTag) {
        log.info("收到活动创建消息：{}", activity);
        try {
            // TODO: 这里可以添加活动创建后的处理逻辑
            // 例如：发送通知、更新缓存、触发 AI 学习等

            // 手动 ACK 确认
            channel.basicAck(deliveryTag, false);
            log.info("活动创建消息处理成功");
        } catch (Exception e) {
            log.error("处理活动创建消息失败：{}", e.getMessage(), e);
            try {
                // 重试次数耗尽后，拒绝消息并放入死信队列
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("拒绝消息失败：{}", ex.getMessage(), ex);
            }
        }
    }
}
