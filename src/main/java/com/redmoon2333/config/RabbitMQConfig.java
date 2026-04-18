package com.redmoon2333.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * RabbitMQ 配置类
 * 定义交换机、队列、绑定关系
 *
 * 已禁用：如需启用，请：
 * 1. 将 @Profile("rabbitmq-disabled") 改为 @Profile("rabbitmq")
 * 2. 在 application.yml 中添加 rabbitmq profile
 * 3. 启动 RabbitMQ Docker 容器
 */
@Profile("rabbitmq")
@Configuration
public class RabbitMQConfig {

    // ==================== 活动相关 ====================

    /**
     * 活动交换机 (Topic 类型)
     */
    @Bean
    public TopicExchange activityExchange() {
        return new TopicExchange("activity.exchange", true, false);
    }

    /**
     * 活动创建队列
     */
    @Bean
    public Queue activityCreatedQueue() {
        return QueueBuilder.durable("activity.created.queue")
                .withArgument("x-message-ttl", 600000) // 10 分钟过期
                .build();
    }

    /**
     * 活动创建队列绑定
     */
    @Bean
    public Binding activityCreatedBinding(Queue activityCreatedQueue, TopicExchange activityExchange) {
        return BindingBuilder.bind(activityCreatedQueue)
                .to(activityExchange)
                .with("activity.created");
    }

    // ==================== 用户相关 ====================

    /**
     * 用户交换机 (Topic 类型)
     */
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange("user.exchange", true, false);
    }

    /**
     * 用户注册队列
     */
    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder.durable("user.registered.queue")
                .withArgument("x-message-ttl", 300000) // 5 分钟过期
                .build();
    }

    /**
     * 用户注册队列绑定
     */
    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange userExchange) {
        return BindingBuilder.bind(userRegisteredQueue)
                .to(userExchange)
                .with("user.registered");
    }

    // ==================== 死信队列 ====================

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("dead.letter.exchange", true, false);
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dead.letter.queue").build();
    }

    /**
     * 死信队列绑定
     */
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with("dead");
    }

    // ==================== 消息转换器 ====================

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        // 开启发送确认
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 消息发送失败，记录日志或重试
            }
        });
        return rabbitTemplate;
    }
}
