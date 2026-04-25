package com.redmoon2333.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 定义交换机、队列、绑定关系
 */
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

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("dead.letter.exchange", true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dead.letter.queue").build();
    }

    // ==================== 死信队列绑定 ====================
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with("dead");
    }

    // ==================== 通知系统 (Fanout) ====================

    @Bean
    public FanoutExchange notifyExchange() {
        return new FanoutExchange("notify.fanout", true, false);
    }

    @Bean
    public Queue notifyEmailQueue() {
        return QueueBuilder.durable("notify.email.queue").build();
    }

    @Bean
    public Queue notifyInappQueue() {
        return QueueBuilder.durable("notify.inapp.queue").build();
    }

    @Bean
    public Binding notifyEmailBinding(Queue notifyEmailQueue, FanoutExchange notifyExchange) {
        return BindingBuilder.bind(notifyEmailQueue).to(notifyExchange);
    }

    @Bean
    public Binding notifyInappBinding(Queue notifyInappQueue, FanoutExchange notifyExchange) {
        return BindingBuilder.bind(notifyInappQueue).to(notifyExchange);
    }

    // ==================== 催促延时队列 (TTL + DLX) ====================

    @Bean
    public Queue taskRemindTtlQueue() {
        return QueueBuilder.durable("task.remind.ttl.queue")
                .withArgument("x-dead-letter-exchange", "dead.letter.exchange")
                .withArgument("x-dead-letter-routing-key", "task.remind")
                .build();
    }

    @Bean
    public DirectExchange taskRemindTtlExchange() {
        return new DirectExchange("task.remind.ttl.exchange", true, false);
    }

    @Bean
    public Binding taskRemindTtlBinding(Queue taskRemindTtlQueue, DirectExchange taskRemindTtlExchange) {
        return BindingBuilder.bind(taskRemindTtlQueue)
                .to(taskRemindTtlExchange)
                .with("task.remind");
    }

    @Bean
    public Queue taskRemindQueue() {
        return QueueBuilder.durable("task.remind.queue").build();
    }

    @Bean
    public Binding taskRemindBinding(Queue taskRemindQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(taskRemindQueue)
                .to(deadLetterExchange)
                .with("task.remind");
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
     * RabbitListener 容器工厂配置
     * 使用 JSON 消息转换器，与发送端保持一致
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        // 消息确认模式：手动确认
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        // 预取数量，每次只获取一条消息处理
        factory.setPrefetchCount(1);
        return factory;
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
