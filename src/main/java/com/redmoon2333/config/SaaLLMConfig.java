package com.redmoon2333.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * LLM配置类（ECNU重构版）
 *
 * 架构说明：
 * - 统一配置所有ChatClient实例
 * - 使用Spring AI OpenAI API连接ECNU OpenAI兼容接口
 * - 对话记忆通过Advisor自动注入
 *
 * ECNU模型选择：
 * - ecnu-plus: 通用对话模型，适合日常对话和RAG场景
 * - ecnu-max: 最强推理模型，支持function_call，适合工具调用
 *
 * Why: ECNU API 是 OpenAI 兼容接口，使用 OpenAI 模块而非 DashScope 模块
 */
@Configuration
public class SaaLLMConfig {

    private static final String ECNU_CHAT_MODEL = "ecnu-plus";
    private static final String ECNU_TOOL_MODEL = "ecnu-max";

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    private OpenAiApi createOpenAiApi() {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
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
     * 主ChatClient（带对话记忆）
     * 用于普通对话和RAG场景
     * 使用ecnu-plus模型
     */
    @Bean
    @Primary
    public ChatClient ecnuChatClient(ChatMemory chatMemory) {
        return ChatClient.builder(createChatModel(ECNU_CHAT_MODEL))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
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
