package com.redmoon2333.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * LLM配置类（精简重构版）
 *
 * 架构说明：
 * - 统一配置所有ChatClient实例
 * - 使用Spring AI 1.1.x的简化API
 * - 对话记忆通过Advisor自动注入
 *
 * Why: 将原有的120+行代码精简到80行以内，消除重复的配置逻辑
 */
@Configuration
public class SaaLLMConfig {

    private static final String QWEN_MODEL = "qwen-plus";
    private static final String TOOL_MODEL = "qwen-max";

    @Value("${aliyun.dashscope.api-key}")
    private String apiKey;

    /**
     * 创建基础DashScope API客户端
     */
    private DashScopeApi createDashScopeApi() {
        return DashScopeApi.builder().apiKey(apiKey).build();
    }

    /**
     * 创建基础ChatModel
     */
    private ChatModel createChatModel(String modelName) {
        return DashScopeChatModel.builder()
                .dashScopeApi(createDashScopeApi())
                .defaultOptions(DashScopeChatOptions.builder().withModel(modelName).build())
                .build();
    }

    /**
     * 主ChatClient（带对话记忆）
     * 用于普通对话和RAG场景
     */
    @Bean
    @Primary
    public ChatClient qwenChatClient(ChatMemory chatMemory) {
        return ChatClient.builder(createChatModel(QWEN_MODEL))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * 工具调用专用ChatClient（无对话记忆）
     * Why: 工具调用使用独立上下文，避免中间结果污染用户对话历史
     */
    @Bean
    public ChatClient qwenToolChatClient() {
        return ChatClient.builder(createChatModel(TOOL_MODEL)).build();
    }

    /**
     * 策划案生成专用ChatClient（无对话记忆）
     * Why: 策划案生成是独立的一次性任务，不需要对话上下文
     */
    @Bean
    public ChatClient planGeneratorChatClient() {
        return ChatClient.builder(createChatModel(QWEN_MODEL)).build();
    }
}
