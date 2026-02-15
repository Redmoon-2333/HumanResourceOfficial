package com.redmoon2333.service;

import com.redmoon2333.config.PromptConfig;
import com.redmoon2333.dto.ChatResponse;
import com.redmoon2333.dto.PlanGeneratorRequest;
import com.redmoon2333.util.MarkdownFormatter;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;



/**
 * AI聊天服务（精简重构版）
 *
 * 架构说明：
 * - 单一职责：仅负责协调对话流程，具体逻辑下沉到策略和配置
 * - RAG检索：使用 Spring AI 原生的 RetrievalAugmentationAdvisor
 * - 工具调用：使用 @Tool 注解，AI自动决定何时调用
 * - 对话记忆：由 ChatClient 内置的 MessageChatMemoryAdvisor 自动处理
 * - 提示词管理：从配置文件加载，支持热更新
 *
 * Why: 将原有的350+行代码精简到150行以内，消除重复逻辑，提高可维护性
 */
@Service
public class AIChatService {
    private static final Logger logger = LoggerFactory.getLogger(AIChatService.class);

    @Resource(name = "qwenChatClient")
    private ChatClient chatClient;

    @Resource(name = "qwenToolChatClient")
    private ChatClient toolChatClient;

    @Resource(name = "planGeneratorChatClient")
    private ChatClient planGeneratorChatClient;

    @Autowired
    private PromptConfig promptConfig;

    @Autowired(required = false)
    private VectorStore vectorStore;

    @Autowired(required = false)
    private ToolService toolService;

    /**
     * 普通对话（同步）
     */
    public ChatResponse chat(Integer userId, String message) {
        logger.info("用户 {} 发送消息", userId);

        // Why: 使用userId作为conversationId，确保每个用户的对话历史独立隔离
        String conversationId = "user_" + userId;

        String response = chatClient.prompt()
                .system(promptConfig.getSystemPrompt())
                .user(message)
                .advisors(advisor -> advisor.param("chat_memory_conversation_id", conversationId))
                .call()
                .content();

        // 后处理：清理Markdown格式问题
        response = MarkdownFormatter.format(response);

        logger.info("AI响应用户 {}: {} 字符", userId, response != null ? response.length() : 0);
        return new ChatResponse(response, "user_" + userId);
    }

    /**
     * 普通对话（流式）
     */
    public Flux<String> chatStream(Integer userId, String message) {
        logger.info("用户 {} 发送流式消息", userId);

        // Why: 使用userId作为conversationId，确保每个用户的对话历史独立隔离
        String conversationId = "user_" + userId;

        // 使用缓冲区处理流式内容的格式化
        StringBuilder formatBuffer = new StringBuilder();

        return chatClient.prompt()
                .system(promptConfig.getSystemPrompt())
                .user(message)
                .advisors(advisor -> advisor.param("chat_memory_conversation_id", conversationId))
                .stream()
                .content()
                .limitRate(100)
                // 后处理：清理Markdown格式问题
                .map(chunk -> MarkdownFormatter.formatStreamChunk(chunk, formatBuffer))
                .doOnComplete(() -> {
                    // 刷新缓冲区剩余内容
                    String remaining = MarkdownFormatter.flushStreamBuffer(formatBuffer);
                    if (!remaining.isEmpty()) {
                        // 这里无法直接发送，需要通过其他方式处理
                        logger.debug("流式响应结束，剩余内容: {} 字符", remaining.length());
                    }
                })
                .doOnError(e -> logger.error("流式对话错误，用户ID: {}", userId, e))
                .onErrorResume(e -> isClientDisconnect(e) ? Flux.empty() : Flux.error(e))
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal ->
                                logger.warn("流式对话失败，用户ID: {}，正在进行第 {} 次重试", userId, retrySignal.totalRetries() + 1)));
    }

    /**
     * RAG增强对话（流式，可选工具调用）
     *
     * Why: 合并原有的多个重载方法，通过参数控制功能开关，减少代码重复
     */
    public Flux<String> chatWithRag(Integer userId, String message, boolean useRAG, boolean enableTools) {
        logger.info("用户 {} 发送消息, RAG: {}, Tools: {}", userId, message, useRAG, enableTools);

        // 工具调用模式：使用独立ChatClient避免污染对话历史
        if (enableTools && toolService != null) {
            return handleToolCalling(message, userId);
        }

        // Why: 使用userId作为conversationId，确保每个用户的对话历史独立隔离
        String conversationId = "user_" + userId;

        // 使用缓冲区处理流式内容的格式化
        StringBuilder formatBuffer = new StringBuilder();

        // 标准RAG模式
        var promptSpec = chatClient.prompt()
                .system(promptConfig.getSystemPrompt())
                .user(message)
                .advisors(advisor -> advisor.param("chat_memory_conversation_id", conversationId));

        // 动态添加RAG Advisor
        if (useRAG && vectorStore != null) {
            promptSpec = promptSpec.advisors(buildRagAdvisor());
        }

        return promptSpec.stream()
                .content()
                .limitRate(100)
                // 后处理：清理Markdown格式问题
                .map(chunk -> MarkdownFormatter.formatStreamChunk(chunk, formatBuffer))
                .doOnComplete(() -> {
                    String remaining = MarkdownFormatter.flushStreamBuffer(formatBuffer);
                    if (!remaining.isEmpty()) {
                        logger.debug("RAG流式响应结束，剩余内容: {} 字符", remaining.length());
                    }
                })
                .doOnError(e -> logger.error("RAG对话错误，用户ID: {}", userId, e))
                .onErrorResume(e -> isClientDisconnect(e) ? Flux.empty() : Flux.error(e))
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal ->
                                logger.warn("RAG对话失败，用户ID: {}，正在进行第 {} 次重试", userId, retrySignal.totalRetries() + 1)));
    }

    /**
     * 生成活动策划案（同步）
     * 使用无记忆的ChatClient，每次生成都是独立的
     */
    public String generatePlan(PlanGeneratorRequest request) {
        logger.info("生成策划案，主题: {}", request.getTheme());
        String response = planGeneratorChatClient.prompt(buildPlanPrompt(request)).call().content();
        // 后处理：清理Markdown格式问题
        return MarkdownFormatter.format(response);
    }

    /**
     * 生成活动策划案（流式）
     * 使用无记忆的ChatClient，每次生成都是独立的
     *
     * Why: 添加重试机制处理网络瞬断问题
     * Warning: 仅对网络相关错误进行重试，业务错误不重试
     */
    public Flux<String> generatePlanStream(PlanGeneratorRequest request) {
        logger.info("流式生成策划案，主题: {}", request.getTheme());

        // 使用缓冲区处理流式内容的格式化
        StringBuilder formatBuffer = new StringBuilder();

        return planGeneratorChatClient.prompt(buildPlanPrompt(request))
                .stream()
                .content()
                .limitRate(100)
                // 后处理：清理Markdown格式问题
                .map(chunk -> MarkdownFormatter.formatStreamChunk(chunk, formatBuffer))
                .doOnComplete(() -> {
                    String remaining = MarkdownFormatter.flushStreamBuffer(formatBuffer);
                    if (!remaining.isEmpty()) {
                        logger.debug("策划案流式响应结束，剩余内容: {} 字符", remaining.length());
                    }
                })
                .doOnError(e -> logger.error("策划案生成错误", e))
                .onErrorResume(e -> isClientDisconnect(e) ? Flux.empty() : Flux.error(e))
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal ->
                            logger.warn("策划案生成失败，正在进行第 {} 次重试", retrySignal.totalRetries() + 1)));
    }

    /**
     * 判断错误是否可重试
     *
     * Why: 只有网络瞬断类错误才值得重试，业务逻辑错误重试无意义
     */
    private boolean isRetryableError(Throwable error) {
        if (error == null || error.getMessage() == null) return false;
        String msg = error.getMessage().toLowerCase();
        return msg.contains("connection reset")
                || msg.contains("connection refused")
                || msg.contains("timeout")
                || msg.contains("broken pipe")
                || msg.contains("unexpected end of stream")
                || msg.contains("ssl");
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 处理工具调用
     *
     * Why: 工具调用使用同步模式获取结果，再模拟流式输出，保持接口一致性
     */
    private Flux<String> handleToolCalling(String message, Integer userId) {
        return Flux.defer(() -> {
            try {
                ToolCallback[] tools = ToolCallbacks.from(toolService);
                logger.info("调用工具，共 {} 个", tools.length);

                String response = toolChatClient.prompt()
                        .system(promptConfig.getSystemPromptWithTools())
                        .user(message)
                        .toolCallbacks(tools)  // Spring AI 1.1.x 使用 toolCallbacks() 方法
                        .call()
                        .content();

                // 后处理：清理Markdown格式问题
                response = MarkdownFormatter.format(response != null ? response : "");

                return simulateStream(response);
            } catch (Exception e) {
                logger.error("工具调用失败", e);
                return Flux.error(e);
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    /**
     * 构建RAG Advisor
     */
    private RetrievalAugmentationAdvisor buildRagAdvisor() {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .topK(5)
                        .build())
                .build();
    }

    /**
     * 构建策划案提示词
     */
    private Prompt buildPlanPrompt(PlanGeneratorRequest request) {
        String planTemplate = promptConfig.getPlanGeneratorPrompt();
        PromptTemplate template = new PromptTemplate(planTemplate);
        Map<String, Object> params = new HashMap<>();
        params.put("theme", request.getTheme());
        params.put("organizer", request.getOrganizer());
        params.put("eventTime", request.getEventTime());
        params.put("eventLocation", request.getEventLocation());
        params.put("staff", request.getStaff());
        params.put("participants", request.getParticipants());
        params.put("purpose", request.getPurpose());
        params.put("leaderCount", request.getLeaderCount());
        params.put("memberCount", request.getMemberCount());
        return template.create(params);
    }

    /**
     * 模拟流式输出（将同步结果转为流式）
     */
    private Flux<String> simulateStream(String text) {
        if (text == null || text.isEmpty()) {
            return Flux.empty();
        }
        return Flux.fromArray(text.split("\n"))
                .map(line -> line + "\n")
                .delayElements(Duration.ofMillis(50));
    }

    /**
     * 判断是否为客户端断开连接异常
     */
    private boolean isClientDisconnect(Throwable error) {
        if (error == null || error.getMessage() == null) return false;
        String msg = error.getMessage();
        return msg.contains("ClientAbortException")
                || msg.contains("Broken pipe")
                || msg.contains("Connection reset")
                || msg.contains("你的主机中的软件中止了一个已建立的连接");
    }
}
