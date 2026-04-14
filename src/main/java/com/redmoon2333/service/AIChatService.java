package com.redmoon2333.service;

import com.redmoon2333.config.PromptConfig;
import com.redmoon2333.config.RedisChatMemory;
import com.redmoon2333.dto.ChatResponse;
import com.redmoon2333.dto.PlanGeneratorRequest;
import com.redmoon2333.util.MarkdownFormatter;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
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
import java.util.List;
import java.util.Map;

/**
 * AI聊天服务（精简重构版）
 *
 * 架构说明：
 * - 单一职责：仅负责协调对话流程，具体逻辑下沉到策略和配置
 * - RAG检索：使用 Spring AI 原生的 RetrievalAugmentationAdvisor
 * - 工具调用：使用 @Tool 注解，AI自动决定何时调用
 * - 对话记忆：手动管理（不使用 MessageChatMemoryAdvisor）
 * - 提示词管理：从配置文件加载，支持热更新
 *
 * Why: Spring AI 1.0.0 的 MessageChatMemoryAdvisor 不实现 StreamAroundAdvisor，
 *      调用 .stream() 时会抛出 IllegalStateException: No StreamAdvisors available to execute
 *      因此改为手动管理对话记忆，同时兼容同步和流式调用
 */
@Service
public class AIChatService {
    private static final Logger logger = LoggerFactory.getLogger(AIChatService.class);

    @Resource(name = "ecnuChatClient")
    private ChatClient chatClient;

    @Resource(name = "ecnuToolChatClient")
    private ChatClient toolChatClient;

    @Resource(name = "planGeneratorChatClient")
    private ChatClient planGeneratorChatClient;

    @Autowired
    private PromptConfig promptConfig;

    @Autowired(required = false)
    private VectorStore vectorStore;

    @Autowired(required = false)
    private ToolService toolService;

    @Autowired
    private RedisChatMemory chatMemory;

    /**
     * 普通对话（同步）
     *
     * Why: 手动管理对话记忆，不依赖 MessageChatMemoryAdvisor
     *      1. 从 RedisChatMemory 获取历史消息
     *      2. 将历史消息注入到 prompt 中
     *      3. 调用完成后保存用户消息和AI回复到记忆
     */
    public ChatResponse chat(Integer userId, String message) {
        logger.info("用户 {} 发送消息", userId);

        String conversationId = "user_" + userId;

        List<Message> history = chatMemory.get(conversationId);

        String response = chatClient.prompt()
                .system(promptConfig.getSystemPrompt())
                .messages(history.toArray(new Message[0]))
                .user(message)
                .call()
                .content();

        response = MarkdownFormatter.format(response);

        chatMemory.add(conversationId, List.of(
                new UserMessage(message),
                new AssistantMessage(response)
        ));

        logger.info("AI响应用户 {}: {} 字符", userId, response != null ? response.length() : 0);
        return new ChatResponse(response, conversationId);
    }

    /**
     * 普通对话（流式）
     *
     * Why: 手动管理对话记忆，流式完成后保存完整对话到 RedisChatMemory
     *      使用 StringBuilder 收集完整响应，在 doOnComplete 中保存
     */
    public Flux<String> chatStream(Integer userId, String message) {
        logger.info("用户 {} 发送流式消息", userId);

        String conversationId = "user_" + userId;

        List<Message> history = chatMemory.get(conversationId);

        StringBuilder formatBuffer = new StringBuilder();
        StringBuilder fullResponse = new StringBuilder();

        return chatClient.prompt()
                .system(promptConfig.getSystemPrompt())
                .messages(history.toArray(new Message[0]))
                .user(message)
                .stream()
                .content()
                .limitRate(100)
                .map(chunk -> {
                    fullResponse.append(chunk);
                    return MarkdownFormatter.formatStreamChunk(chunk, formatBuffer);
                })
                .filter(chunk -> !chunk.isEmpty())
                .concatWith(Flux.defer(() -> {
                    String remaining = MarkdownFormatter.flushStreamBuffer(formatBuffer);
                    if (!remaining.isEmpty()) {
                        logger.debug("流式响应结束，发送剩余内容: {} 字符", remaining.length());
                        return Flux.just(remaining);
                    }
                    return Flux.empty();
                }))
                .doOnComplete(() -> {
                    chatMemory.add(conversationId, List.of(
                            new UserMessage(message),
                            new AssistantMessage(fullResponse.toString())
                    ));
                    logger.debug("流式对话记忆已保存，用户ID: {}", userId);
                })
                .doOnError(e -> logger.error("流式对话错误，用户ID: {}", userId, e))
                .onErrorResume(e -> isClientDisconnect(e) ? Flux.empty() : Flux.error(e))
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal -> {
                            fullResponse.setLength(0);
                            formatBuffer.setLength(0);
                            logger.warn("流式对话失败，用户ID: {}，正在进行第 {} 次重试", userId, retrySignal.totalRetries() + 1);
                        }));
    }

    /**
     * RAG增强对话（流式，可选工具调用）
     *
     * Why: 合并原有的多个重载方法，通过参数控制功能开关，减少代码重复
     */
    public Flux<String> chatWithRag(Integer userId, String message, boolean useRAG, boolean enableTools) {
        logger.info("用户 {} 发送消息, RAG: {}, Tools: {}", userId, message, useRAG, enableTools);

        if (enableTools && toolService != null) {
            return handleToolCalling(message, userId);
        }

        String conversationId = "user_" + userId;

        List<Message> history = chatMemory.get(conversationId);

        StringBuilder formatBuffer = new StringBuilder();
        StringBuilder fullResponse = new StringBuilder();

        var promptSpec = chatClient.prompt()
                .system(promptConfig.getSystemPrompt())
                .messages(history.toArray(new Message[0]))
                .user(message);

        if (useRAG && vectorStore != null) {
            promptSpec = promptSpec.advisors(buildRagAdvisor());
        }

        return promptSpec.stream()
                .content()
                .limitRate(100)
                .map(chunk -> {
                    fullResponse.append(chunk);
                    return MarkdownFormatter.formatStreamChunk(chunk, formatBuffer);
                })
                .filter(chunk -> !chunk.isEmpty())
                .concatWith(Flux.defer(() -> {
                    String remaining = MarkdownFormatter.flushStreamBuffer(formatBuffer);
                    if (!remaining.isEmpty()) {
                        logger.debug("RAG流式响应结束，发送剩余内容: {} 字符", remaining.length());
                        return Flux.just(remaining);
                    }
                    return Flux.empty();
                }))
                .doOnComplete(() -> {
                    chatMemory.add(conversationId, List.of(
                            new UserMessage(message),
                            new AssistantMessage(fullResponse.toString())
                    ));
                    logger.debug("RAG对话记忆已保存，用户ID: {}", userId);
                })
                .doOnError(e -> logger.error("RAG对话错误，用户ID: {}", userId, e))
                .onErrorResume(e -> isClientDisconnect(e) ? Flux.empty() : Flux.error(e))
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal -> {
                            fullResponse.setLength(0);
                            formatBuffer.setLength(0);
                            logger.warn("RAG对话失败，用户ID: {}，正在进行第 {} 次重试", userId, retrySignal.totalRetries() + 1);
                        }));
    }

    /**
     * 检测ChatECNU模型是否支持tool功能
     */
    private boolean isToolSupported() {
        return true;
    }

    /**
     * 生成活动策划案（同步）
     */
    public String generatePlan(PlanGeneratorRequest request) {
        logger.info("生成策划案，主题: {}", request.getTheme());
        String response = planGeneratorChatClient.prompt(buildPlanPrompt(request)).call().content();
        return MarkdownFormatter.format(response);
    }

    /**
     * 生成活动策划案（流式）
     */
    public Flux<String> generatePlanStream(PlanGeneratorRequest request) {
        logger.info("流式生成策划案，主题: {}", request.getTheme());

        StringBuilder formatBuffer = new StringBuilder();

        return planGeneratorChatClient.prompt(buildPlanPrompt(request))
                .stream()
                .content()
                .limitRate(100)
                .map(chunk -> MarkdownFormatter.formatStreamChunk(chunk, formatBuffer))
                .filter(chunk -> !chunk.isEmpty())
                .concatWith(Flux.defer(() -> {
                    String remaining = MarkdownFormatter.flushStreamBuffer(formatBuffer);
                    if (!remaining.isEmpty()) {
                        logger.debug("策划案流式响应结束，发送剩余内容: {} 字符", remaining.length());
                        return Flux.just(remaining);
                    }
                    return Flux.empty();
                }))
                .doOnError(e -> logger.error("策划案生成错误", e))
                .onErrorResume(e -> isClientDisconnect(e) ? Flux.empty() : Flux.error(e))
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retrySignal ->
                            logger.warn("策划案生成失败，正在进行第 {} 次重试", retrySignal.totalRetries() + 1)));
    }

    private boolean isRetryableError(Throwable error) {
        if (error == null || error.getMessage() == null) return false;
        String msg = error.getMessage().toLowerCase();
        return msg.contains("connection reset")
                || msg.contains("connection refused")
                || msg.contains("timeout")
                || msg.contains("broken pipe")
                || msg.contains("unexpected end of stream")
                || msg.contains("ssl")
                || msg.contains("premature close")
                || msg.contains("prematurecloseexception");
    }

    // ==================== 私有辅助方法 ====================

    private Flux<String> handleToolCalling(String message, Integer userId) {
        return Flux.defer(() -> {
            try {
                if (!isToolSupported()) {
                    String errorMsg = "当前ChatECNU模型不支持工具调用功能";
                    logger.warn(errorMsg);
                    return simulateStream(errorMsg);
                }

                ToolCallback[] tools = ToolCallbacks.from(toolService);
                logger.info("调用工具，共 {} 个", tools.length);

                String response = toolChatClient.prompt()
                        .system(promptConfig.getSystemPromptWithTools())
                        .user(message)
                        .toolCallbacks(tools)
                        .call()
                        .content();

                response = MarkdownFormatter.format(response != null ? response : "");

                return simulateStream(response);
            } catch (Exception e) {
                logger.error("工具调用失败", e);
                return Flux.error(e);
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private RetrievalAugmentationAdvisor buildRagAdvisor() {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .topK(5)
                        .build())
                .build();
    }

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

    private Flux<String> simulateStream(String text) {
        if (text == null || text.isEmpty()) {
            return Flux.empty();
        }
        return Flux.fromArray(text.split("\n"))
                .map(line -> line + "\n")
                .delayElements(Duration.ofMillis(50));
    }

    private boolean isClientDisconnect(Throwable error) {
        if (error == null || error.getMessage() == null) return false;
        String msg = error.getMessage();
        return msg.contains("ClientAbortException")
                || msg.contains("Broken pipe")
                || msg.contains("Connection reset")
                || msg.contains("你的主机中的软件中止了一个已建立的连接");
    }
}
