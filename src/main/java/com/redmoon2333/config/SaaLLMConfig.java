package com.redmoon2333.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * LLM配置类（ECNU重构版）
 *
 * 架构说明：
 * - 统一配置所有ChatClient实例
 * - 使用Spring AI OpenAI API连接ECNU OpenAI兼容接口
 * - 对话记忆由AIChatService手动管理（不使用defaultAdvisors）
 *
 * ECNU模型选择：
 * - ecnu-plus: 通用对话模型，适合日常对话和RAG场景
 * - ecnu-max: 最强推理模型，支持function_call，适合工具调用
 *
 * Why: ECNU API 是 OpenAI 兼容接口，使用 OpenAI 模块而非 DashScope 模块
 *
 * 重要：
 * 1. 流式响应可能需要较长时间，必须配置足够的超时时间
 * 2. Spring AI 1.0.0 的 MessageChatMemoryAdvisor 不支持 StreamAroundAdvisor，
 *    因此不使用 defaultAdvisors，改为在 Service 层手动管理对话记忆
 */
@Configuration
public class SaaLLMConfig {

    private static final String ECNU_CHAT_MODEL = "ecnu-plus";
    private static final String ECNU_TOOL_MODEL = "ecnu-max";

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.timeout.connect:30s}")
    private Duration connectTimeout;

    @Value("${spring.ai.openai.timeout.read:300s}")
    private Duration readTimeout;

    /**
     * 创建带超时配置的 WebClient.Builder
     *
     * Why: Spring AI 1.0.0 默认的 WebClient 超时配置不足
     *      流式响应可能需要几分钟，必须配置足够的读取超时
     *      否则会出现 PrematureCloseException: Connection prematurely closed DURING response
     *
     * Warning: 不使用 @Bean 避免与 Spring AI 自动配置冲突
     */
    private WebClient.Builder createWebClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeout.toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(connectTimeout.toMillis(), TimeUnit.MILLISECONDS)))
                .responseTimeout(readTimeout);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    private OpenAiApi createOpenAiApi() {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .webClientBuilder(createWebClientBuilder())
                .build();
    }

    private ChatModel createChatModel(String modelName) {
        return OpenAiChatModel.builder()
                .openAiApi(createOpenAiApi())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(modelName)
                        .temperature(0.7)
                        .maxTokens(2000)
                        .build())
                .build();
    }

    /**
     * 主ChatClient（无默认Advisor）
     *
     * Why: 不使用 defaultAdvisors(MessageChatMemoryAdvisor)
     *      因为 Spring AI 1.0.0 的 MessageChatMemoryAdvisor 不实现 StreamAroundAdvisor
     *      调用 .stream() 时会抛出 IllegalStateException: No StreamAdvisors available to execute
     *      对话记忆改为在 AIChatService 中手动管理
     */
    @Bean
    @Primary
    public ChatClient ecnuChatClient() {
        return ChatClient.builder(createChatModel(ECNU_CHAT_MODEL)).build();
    }

    /**
     * 工具调用专用ChatClient（无对话记忆）
     * 使用ecnu-max模型（支持function_call）
     * Why: 工具调用使用独立上下文，避免中间结果污染用户对话历史
     */
    @Bean
    public ChatClient ecnuToolChatClient() {
        return ChatClient.builder(createChatModel(ECNU_TOOL_MODEL)).build();
    }

    /**
     * 策划案生成专用ChatClient（无对话记忆）
     * 使用ecnu-plus模型
     * Why: 策划案生成是独立的一次性任务，不需要对话上下文
     */
    @Bean
    public ChatClient planGeneratorChatClient() {
        return ChatClient.builder(createChatModel(ECNU_CHAT_MODEL)).build();
    }
}
