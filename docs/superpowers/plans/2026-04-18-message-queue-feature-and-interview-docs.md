# 消息队列功能实现与面试文档补充计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为项目添加消息队列功能（RabbitMQ），并在面试宝典中补充消息队列相关知识点

**Architecture:** 
- 采用 RabbitMQ 作为消息队列中间件
- 通过 Spring AMQP 实现消息发送与监听
- 使用注解+切面实现声明式消息发送
- 面试文档补充到模块 7 和模块 9

**Tech Stack:** Spring Boot 3.3.5, RabbitMQ, Spring AMQP, Redis

---

## 第一部分：消息队列代码实现

### Task 1: 添加 RabbitMQ 依赖与配置

**Files:**
- Modify: `pom.xml`
- Create: `src/main/resources/application-rabbitmq.yml`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 添加 RabbitMQ 依赖到 pom.xml**

在 `<dependencies>` 中添加:

```xml
<!-- RabbitMQ -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

- [ ] **Step 2: 创建 application-rabbitmq.yml 配置文件**

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        acknowledge-mode: manual  # 手动 ACK
        prefetch: 10  # 每次预取 10 条消息
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 3
          max-interval: 10000
    template:
      retry:
        enabled: true
        initial-interval: 1000
        max-attempts: 3
```

- [ ] **Step 3: 修改 application.yml 导入新配置**

在 `spring.profiles.active` 中添加 `rabbitmq`:

```yaml
spring:
  profiles:
    active: dev,rabbitmq
```

- [ ] **Step 4: 提交**

```bash
git add pom.xml src/main/resources/application.yml src/main/resources/application-rabbitmq.yml
git commit -m "feat: add RabbitMQ dependency and configuration"
```

---

### Task 2: 创建 RabbitMQ 配置类

**Files:**
- Create: `src/main/java/com/redmoon2333/config/RabbitMQConfig.java`

- [ ] **Step 1: 创建 RabbitMQConfig 配置类**

```java
package com.redmoon2333.config;

import org.springframework.amqp.core.*;
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
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/redmoon2333/config/RabbitMQConfig.java
git commit -m "feat: add RabbitMQ configuration with exchanges and queues"
```

---

### Task 3: 创建消息发送工具类

**Files:**
- Create: `src/main/java/com/redmoon2333/util/MQSender.java`

- [ ] **Step 1: 创建 MQSender 工具类**

```java
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
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/redmoon2333/util/MQSender.java
git commit -m "feat: add MQSender utility class for message sending"
```

---

### Task 4: 创建活动消息监听器

**Files:**
- Create: `src/main/java/com/redmoon2333/listener/ActivityMessageListener.java`

- [ ] **Step 1: 创建 ActivityMessageListener 监听器**

```java
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
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/redmoon2333/listener/ActivityMessageListener.java
git commit -m "feat: add ActivityMessageListener for activity events"
```

---

### Task 5: 创建用户消息监听器

**Files:**
- Create: `src/main/java/com/redmoon2333/listener/UserMessageListener.java`

- [ ] **Step 1: 创建 UserMessageListener 监听器**

```java
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
 * 用户消息监听器
 * 监听用户相关事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserMessageListener {

    /**
     * 监听用户注册事件
     *
     * @param userInfo 用户信息
     * @param channel RabbitMQ channel
     * @param deliveryTag 消息标签
     */
    @RabbitListener(queues = "user.registered.queue")
    public void handleUserRegistered(@Payload Object userInfo, 
                                      Channel channel, 
                                      long deliveryTag) {
        log.info("收到用户注册消息：{}", userInfo);
        try {
            // TODO: 这里可以添加用户注册后的处理逻辑
            // 例如：发送欢迎邮件、发放新人优惠券、初始化用户配置等
            
            // 手动 ACK 确认
            channel.basicAck(deliveryTag, false);
            log.info("用户注册消息处理成功");
        } catch (Exception e) {
            log.error("处理用户注册消息失败：{}", e.getMessage(), e);
            try {
                // 重试次数耗尽后，拒绝消息并放入死信队列
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("拒绝消息失败：{}", ex.getMessage(), ex);
            }
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/redmoon2333/listener/UserMessageListener.java
git commit -m "feat: add UserMessageListener for user events"
```

---

### Task 6: 在 Service 中集成消息队列

**Files:**
- Modify: `src/main/java/com/redmoon2333/service/AuthService.java`
- Modify: `src/main/java/com/redmoon2333/service/ActivityService.java`

- [ ] **Step 1: 修改 AuthService 添加用户注册事件发送**

在 `AuthService` 中添加 `MQSender` 依赖并在注册方法中发送消息:

```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MQSender mqSender;
    // ... 其他依赖

    public Map<String, Object> register(RegisterRequest request) {
        // 原有注册逻辑
        User user = userService.createUser(request);
        
        // 发送用户注册事件
        mqSender.send("user.exchange", "user.registered", Map.of(
            "userId", user.getUserId(),
            "username", user.getUsername(),
            "email", user.getEmail()
        ));
        
        // 返回结果
        // ...
    }
}
```

- [ ] **Step 2: 修改 ActivityService 添加活动创建事件发送**

在 `ActivityService` 中添加 `MQSender` 依赖并在创建方法中发送消息:

```java
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final MQSender mqSender;
    // ... 其他依赖

    @DistributedLock(key = "'activity:create:' + #activity.activityName")
    @CacheEvict(value = "activity:list", key = "'all'")
    public Activity createActivity(Activity activity) {
        Activity created = activityMapper.insert(activity);
        
        // 发送活动创建事件
        mqSender.send("activity.exchange", "activity.created", Map.of(
            "activityId", created.getActivityId(),
            "activityName", created.getActivityName(),
            "startTime", created.getStartTime()
        ));
        
        return created;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/redmoon2333/service/AuthService.java src/main/java/com/redmoon2333/service/ActivityService.java
git commit -m "feat: integrate MQ events in AuthService and ActivityService"
```

---

### Task 7: 创建死信队列配置

**Files:**
- Modify: `src/main/java/com/redmoon2333/config/RabbitMQConfig.java`

- [ ] **Step 1: 在 RabbitMQConfig 中添加死信队列配置**

添加以下配置:

```java
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

/**
 * 活动队列死信配置 (在原有队列上添加死信参数)
 * 需要将 activityCreatedQueue 改为：
 */
@Bean
public Queue activityCreatedQueueWithDLX() {
    return QueueBuilder.durable("activity.created.queue")
            .withArgument("x-message-ttl", 600000)
            .withArgument("x-dead-letter-exchange", "dead.letter.exchange")
            .withArgument("x-dead-letter-routing-key", "dead")
            .build();
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/redmoon2333/config/RabbitMQConfig.java
git commit -m "feat: add dead letter queue configuration"
```

---

### Task 8: 创建消息队列监控接口

**Files:**
- Create: `src/main/java/com/redmoon2333/controller/MQMonitorController.java`

- [ ] **Step 1: 创建 MQMonitorController 监控接口**

```java
package com.redmoon2333.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息队列监控接口
 */
@RestController
@RequestMapping("/api/mq")
@RequiredArgsConstructor
public class MQMonitorController {

    private final ConnectionFactory connectionFactory;

    /**
     * 检查 MQ 连接状态
     */
    @GetMapping("/health")
    public Map<String, Object> checkHealth() {
        Map<String, Object> result = new HashMap<>();
        try {
            connectionFactory.createConnection();
            result.put("status", "UP");
            result.put("message", "RabbitMQ 连接正常");
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("message", "RabbitMQ 连接失败：" + e.getMessage());
        }
        return result;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add src/main/java/com/redmoon2333/controller/MQMonitorController.java
git commit -m "feat: add MQ monitor health check endpoint"
```

---

## 第二部分：面试文档补充

### Task 9: 补充模块 7 消息队列应用场景

**Files:**
- Modify: `docs/面试宝典/07-综合实战与架构设计篇.md`

- [ ] **Step 1: 在 7.4 节扩展项目实战代码示例**

在现有的 7.4 节末尾添加以下内容:

```markdown
### 📁 本项目中的 MQ 实现

**配置类** (RabbitMQConfig.java):

```java
@Configuration
public class RabbitMQConfig {

    // Topic 交换机
    @Bean
    public TopicExchange activityExchange() {
        return new TopicExchange("activity.exchange", true, false);
    }
    
    // 队列配置 (带 TTL 和死信)
    @Bean
    public Queue activityCreatedQueue() {
        return QueueBuilder.durable("activity.created.queue")
                .withArgument("x-message-ttl", 600000) // 10 分钟过期
                .withArgument("x-dead-letter-exchange", "dead.letter.exchange")
                .withArgument("x-dead-letter-routing-key", "dead")
                .build();
    }
    
    // 绑定关系
    @Bean
    public Binding activityCreatedBinding(Queue activityCreatedQueue, 
                                           TopicExchange activityExchange) {
        return BindingBuilder.bind(activityCreatedQueue)
                .to(activityExchange)
                .with("activity.created");
    }
    
    // JSON 消息转换器
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

**Service 发送消息** (ActivityService.java):

```java
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final MQSender mqSender;
    
    @DistributedLock(key = "'activity:create:' + #activity.activityName")
    public Activity createActivity(Activity activity) {
        // 1. 数据库操作
        activityMapper.insert(activity);
        
        // 2. 清除缓存
        // @CacheEvict 自动处理
        
        // 3. 发送 MQ 消息 (异步通知其他服务)
        mqSender.send("activity.exchange", "activity.created", Map.of(
            "activityId", activity.getActivityId(),
            "activityName", activity.getActivityName()
        ));
        
        return activity;
    }
}
```

**监听器处理** (ActivityMessageListener.java):

```java
@Component
public class ActivityMessageListener {

    @RabbitListener(queues = "activity.created.queue")
    public void handleActivityCreated(@Payload Object activity, 
                                       Channel channel, 
                                       long deliveryTag) {
        try {
            // 处理活动创建后的逻辑
            // 例如：发送通知、更新缓存、触发 AI 学习
            
            // 手动 ACK
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 处理失败，拒绝消息
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
```

**可靠性保证**:

| 环节 | 配置 | 作用 |
|------|------|------|
| **生产者** | confirmCallback | 消息到达交换机确认 |
| **交换机→队列** | 队列持久化 | 服务器重启不丢失 |
| **消费者** | manual-ack | 处理成功后再确认 |
| **失败处理** | dead-letter | 失败消息进入死信队列 |
```

- [ ] **Step 2: 提交**

```bash
git add docs/面试宝典/07-综合实战与架构设计篇.md
git commit -m "docs: add MQ implementation details to module 7"
```

---

### Task 10: 补充模块 9 消息队列八股文

**Files:**
- Modify: `docs/面试宝典/09-扩展知识八股文篇.md`

- [ ] **Step 1: 扩展 9.1 节消息队列内容**

在现有的 9.1 节末尾添加以下内容:

```markdown
### ❓ 消息队列如何保证可靠性？

**参考答案**:

**完整链路保证**:

```
┌─────────────────────────────────────────────────────────────┐
│                    MQ 可靠性保证                              │
├─────────────────────────────────────────────────────────────┤
│  1. 生产者不丢失                                              │
│     - 开启 confirm 确认模式                                   │
│     - 消息持久化到磁盘                                        │
│     - 失败重试 (最多 3 次)                                     │
│     - 本地消息表 (事务消息)                                    │
├─────────────────────────────────────────────────────────────┤
│  2. MQ 服务器不丢失                                           │
│     - Exchange 持久化                                          │
│     - Queue 持久化 (durable=true)                              │
│     - Message 持久化 (deliveryMode=2)                          │
│     - 镜像集群 (多副本)                                        │
├─────────────────────────────────────────────────────────────┤
│  3. 消费者不丢失                                              │
│     - 手动 ACK (处理成功后确认)                                │
│     - 处理失败重回队列 (限制重试次数)                          │
│     - 死信队列 (重试耗尽后接收)                                │
│     - 记录日志，人工介入                                      │
└─────────────────────────────────────────────────────────────┘
```

**RabbitMQ 实现**:

```java
// 生产者确认
rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
    if (!ack) {
        log.error("消息发送失败：{}", cause);
        // 记录到数据库，定时重试
    }
});

// 消费者手动 ACK
@RabbitListener(queues = "order.queue")
public void handleMessage(Message message, Channel channel, long deliveryTag) {
    try {
        // 处理业务
        processMessage(message);
        // 成功 ACK
        channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
        try {
            // 失败重试 (false=不重新入队，进入死信)
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException ex) {
            log.error("拒绝消息失败", ex);
        }
    }
}
```

### ❓ 什么是死信队列？如何使用？

**参考答案**:

**死信 (Dead Letter) 来源**:

| 来源 | 说明 |
|------|------|
| **消息被拒绝** | basic.reject/nack 且 requeue=false |
| **消息过期** | TTL 超时 |
| **队列满** | 队列长度达到最大值 |

**死信队列作用**:

```
1. 异常消息隔离：失败消息单独存放，不影响正常消息
2. 问题排查：分析死信消息，定位业务问题
3. 人工处理：运维人员可以查看并手动处理
```

**配置示例**:

```java
// 普通队列配置死信
@Bean
public Queue businessQueue() {
    return QueueBuilder.durable("business.queue")
            .withArgument("x-message-ttl", 600000) // 10 分钟过期
            .withArgument("x-dead-letter-exchange", "dead.letter.exchange")
            .withArgument("x-dead-letter-routing-key", "dead")
            .build();
}

// 死信队列
@Bean
public Queue deadLetterQueue() {
    return QueueBuilder.durable("dead.letter.queue").build();
}
```

### ❓ 消息队列如何选型？

**参考答案**:

**三大 MQ 对比**:

| 特性 | RabbitMQ | Kafka | RocketMQ |
|------|----------|-------|----------|
| **协议** | AMQP | 自定义 | 自定义 |
| **吞吐量** | 1 万/s | 10 万/s | 10 万/s |
| **延迟** | 微秒级 | 毫秒级 | 毫秒级 |
| **可靠性** | 高 (ACK) | 中 | 高 (事务消息) |
| **功能** | 丰富 (路由/优先级/延时) | 简单 | 丰富 (事务/延时/回溯) |
| **生态** | 成熟 | 大数据生态 | 阿里生态 |
| **适用** | 中小系统/复杂路由 | 日志收集/大数据 | 金融/电商核心 |

**选型建议**:

```
场景 1: 复杂路由/延时任务 → RabbitMQ
场景 2: 日志收集/大数据流 → Kafka
场景 3: 订单/支付核心 → RocketMQ
场景 4: 简单异步/已有 Redis → Redis Stream
```
```

- [ ] **Step 2: 提交**

```bash
git add docs/面试宝典/09-扩展知识八股文篇.md
git commit -m "docs: expand MQ interview questions in module 9"
```

---

### Task 11: 更新模块导航 README

**Files:**
- Modify: `docs/面试宝典/README.md`

- [ ] **Step 1: 更新模块总览表格**

将模块总览表格修改为:

| 模块 | 主题 | 题目数 | 完成状态 |
|------|------|--------|----------|
| 模块 1 | [Spring Boot 基础篇](./01-SpringBoot 基础篇.md) | 15 题 | ✅ 完成 |
| 模块 2 | [注解与 AOP 切面篇](./02-注解与 AOP 切面篇.md) | 20 题 | ✅ 完成 |
| 模块 3 | [Redis 缓存与分布式锁篇](./03-Redis 缓存与分布式锁篇.md) | 20 题 | ✅ 完成 |
| 模块 4 | [JWT 认证与安全篇](./04-JWT 认证与安全篇.md) | 15 题 | ✅ 完成 |
| 模块 5 | [Spring AI 与 RAG 篇](./05-SpringAI 与 RAG 篇.md) | 15 题 | ✅ 完成 |
| 模块 6 | [数据库与 MyBatis-Plus 篇](./06-数据库与 MyBatis-Plus 篇.md) | 10 题 | ✅ 完成 |
| 模块 7 | [综合实战与架构设计篇](./07-综合实战与架构设计篇.md) | 18 题 | ✅ 完成 |
| 模块 8 | [代码重构与质量提升篇](./08-代码重构建议.md) | 8 题 | ✅ 完成 |
| 模块 9 | [扩展知识八股文篇](./09-扩展知识八股文篇.md) | 35+ 题 | ✅ 完成 |

- [ ] **Step 2: 在模块 7 描述中添加消息队列**

修改模块 7 的核心考点:

```
**核心考点**:
- 项目架构设计 (分层架构、职责划分)
- 微服务拆分思路 (按业务域拆分)
- 分布式事务方案 (2PC/TCC/Saga/MQ 事务)
- 消息队列应用 (异步/削峰/解耦/延时) **← 新增**
- 性能优化手段 (缓存/限流/索引/连接池)
```

- [ ] **Step 3: 更新完成统计**

将总题目数更新为 `150+ 道`

- [ ] **Step 4: 提交**

```bash
git add docs/面试宝典/README.md
git commit -m "docs: update README with MQ content summary"
```

---

## 计划完成检查

- [ ] 所有代码任务完成 (Task 1-8)
- [ ] 所有文档任务完成 (Task 9-11)
- [ ] 运行测试确保代码正常
- [ ] 提交所有更改到 git

---

## 执行选项

计划已创建完成。两种执行方式：

1. **Subagent-Driven (推荐)** - 为每个任务分派独立的子代理执行，每个任务完成后进行审查，迭代快速
2. **Inline Execution** - 在当前会话中使用 executing-plans 技能批量执行，设置检查点进行审查

选择哪种方式执行此计划？
