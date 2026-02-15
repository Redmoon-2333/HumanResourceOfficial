package com.redmoon2333.controller;

import com.redmoon2333.annotation.RequireMemberRole;
import com.redmoon2333.config.RedisChatMemory;
import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.dto.ChatRequest;
import com.redmoon2333.dto.ChatResponse;
import com.redmoon2333.dto.PlanGeneratorRequest;
import com.redmoon2333.dto.RagChatRequest;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.service.AIChatService;
import com.redmoon2333.util.PermissionUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI聊天控制器
 * 提供AI对话和策划案生成功能，支持基于用户的记忆隔离
 */
@RestController
@RequestMapping("/api/ai")
public class AIChatController {
    private static final Logger logger = LoggerFactory.getLogger(AIChatController.class);

    @Autowired
    private AIChatService aiChatService;

    @Autowired
    private PermissionUtil permissionUtil;

    @Autowired
    private RedisChatMemory chatMemory;
    
    /**
     * AI对话接口（带用户记忆）
     * 每个用户拥有独立的对话上下文和历史记忆
     * 
     * @param request 聊天请求
     * @param httpRequest HTTP请求（用于获取当前用户）
     * @return 聊天响应
     */
    @PostMapping("/chat")
    @RequireMemberRole("使用AI对话功能")
    public ApiResponse<ChatResponse> chat(
            @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {
        logger.info("收到AI对话请求");
        logger.debug("请求内容: 消息={}", request.getMessage());
        
        try {
            // 从JWT token中获取当前用户ID
            Integer userId = (Integer) httpRequest.getAttribute("userId");
            
            if (userId == null) {
                logger.warn("AI对话请求失败：无法获取用户ID，可能未登录或令牌无效");
                return ApiResponse.error("用户未登录", ErrorCode.TOKEN_REQUIRED.getCode());
            }
            
            logger.debug("获取到用户ID: {}", userId);
            
            ChatResponse response = aiChatService.chat(userId, request.getMessage());
            
            logger.info("AI对话请求处理成功，用户ID: {}", userId);
            logger.debug("响应内容长度: {} 字符", response.getResponse().length());
            
            return ApiResponse.success(response);
        } catch (Exception e) {
            logger.error("AI对话请求处理失败: {}", e.getMessage(), e);
            return ApiResponse.error("AI对话失败: " + e.getMessage(), ErrorCode.SYSTEM_ERROR.getCode());
        }
    }
    
    /**
     * AI流式对话接口（带用户记忆）
     * 每个用户拥有独立的对话上下文和历史记忆
     * 
     * @param request 聊天请求
     * @param httpRequest HTTP请求（用于获取当前用户）
     * @return 流式响应
     */
    @PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireMemberRole("使用AI流式对话功能")
    public Flux<String> chatStream(
            @RequestBody ChatRequest request,
            HttpServletRequest httpRequest) {
        logger.info("收到AI流式对话请求");
        logger.debug("请求内容: 消息={}", request.getMessage());
        
        try {
            // 从JWT token中获取当前用户ID
            Integer userId = (Integer) httpRequest.getAttribute("userId");
            
            if (userId == null) {
                logger.warn("AI流式对话请求失败：无法获取用户ID");
                return Flux.just("{\"error\":\"用户未登录\"}");
            }
            
            logger.debug("获取到用户ID: {}", userId);
            
            return aiChatService.chatStream(userId, request.getMessage())
                    .doOnSubscribe(subscription -> logger.info("客户端开始订阅流式响应，用户ID: {}", userId))
                    .doOnComplete(() -> logger.info("流式对话完成，用户ID: {}", userId))
                    .doOnCancel(() -> logger.warn("客户端取消了流式请求，用户ID: {}", userId))
                    .doOnError(error -> {
                        String errorMsg = error.getMessage();
                        // 客户端断开连接不记录为error级别
                        if (errorMsg != null && (errorMsg.contains("ClientAbortException") 
                                || errorMsg.contains("Broken pipe")
                                || errorMsg.contains("Connection reset")
                                || errorMsg.contains("你的主机中的软件中止了一个已建立的连接"))) {
                            logger.warn("客户端断开连接: {}", errorMsg);
                        } else {
                            logger.error("流式对话错误: {}", errorMsg, error);
                        }
                    })
                    .onErrorResume(error -> {
                        String errorMsg = error.getMessage();
                        // 客户端断开连接不返回错误，直接结束流
                        if (errorMsg != null && (errorMsg.contains("ClientAbortException") 
                                || errorMsg.contains("Broken pipe")
                                || errorMsg.contains("Connection reset")
                                || errorMsg.contains("你的主机中的软件中止了一个已建立的连接"))) {
                            return Flux.empty();
                        }
                        // 其他错误返回错误信息
                        return Flux.just("{\"error\":\"" + errorMsg + "\"}");
                    });
        } catch (Exception e) {
            logger.error("AI流式对话请求处理失败: {}", e.getMessage(), e);
            return Flux.just("{\"error\":\"AI对话失败: " + e.getMessage() + "\"}");
        }
    }
    
    /**
     * 生成活动策划案
     * 需要部员及以上权限
     * 
     * @param request 策划案生成请求
     * @return HTML格式的策划案
     */
    @PostMapping("/generate-plan")
    @RequireMemberRole("生成活动策划案")
    public ApiResponse<String> generatePlan(@RequestBody PlanGeneratorRequest request) {
        logger.info("收到生成策划案请求");
        logger.debug("请求参数: 主题={}", request.getTheme());
        
        try {
            // 验证必填参数
            if (request.getTheme() == null || request.getTheme().trim().isEmpty()) {
                logger.warn("生成策划案请求失败：活动主题为空");
                return ApiResponse.error("活动主题不能为空", ErrorCode.INVALID_REQUEST_PARAMETER.getCode());
            }
            
            logger.debug("开始调用AI服务生成策划案");
            String planContent = aiChatService.generatePlan(request);
            
            logger.info("策划案生成成功，主题: {}", request.getTheme());
            logger.debug("生成的策划案内容长度: {} 字符", planContent.length());
            
            return ApiResponse.success(planContent);
        } catch (Exception e) {
            logger.error("生成策划案失败: {}", e.getMessage(), e);
            return ApiResponse.error("生成策划案失败: " + e.getMessage(), ErrorCode.SYSTEM_ERROR.getCode());
        }
    }
    
    /**
     * 流式生成活动策划案
     * 需要部员及以上权限
     * 
     * @param request 策划案生成请求
     * @return 流式输出HTML格式的策划案
     */
    @PostMapping(value = "/generate-plan-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireMemberRole("流式生成活动策划案")
    public Flux<String> generatePlanStream(@RequestBody PlanGeneratorRequest request) {
        logger.info("收到流式生成策划案请求");
        logger.debug("请求参数: 主题={}", request.getTheme());
        
        // 验证必填参数
        if (request.getTheme() == null || request.getTheme().trim().isEmpty()) {
            logger.warn("流式生成策划案请求失败：活动主题为空");
            return Flux.just("{\"error\":\"活动主题不能为空\"}");
        }
        
        logger.debug("开始调用AI服务流式生成策划案");
        return aiChatService.generatePlanStream(request)
                .doOnSubscribe(subscription -> logger.info("客户端开始订阅流式响应"))
                .doOnComplete(() -> logger.info("流式策划案生成完成"))
                .doOnCancel(() -> logger.warn("客户端取消了流式请求"))
                .doOnError(error -> {
                    String errorMsg = error.getMessage();
                    // 客户端断开连接不记录为error级别
                    if (errorMsg != null && (errorMsg.contains("ClientAbortException") 
                            || errorMsg.contains("Broken pipe")
                            || errorMsg.contains("Connection reset")
                            || errorMsg.contains("你的主机中的软件中止了一个已建立的连接"))) {
                        logger.warn("客户端断开连接: {}", errorMsg);
                    } else {
                        logger.error("流式策划案生成错误: {}", errorMsg, error);
                    }
                })
                .onErrorResume(error -> {
                    String errorMsg = error.getMessage();
                    // 客户端断开连接不返回错误，直接结束流
                    if (errorMsg != null && (errorMsg.contains("ClientAbortException") 
                            || errorMsg.contains("Broken pipe")
                            || errorMsg.contains("Connection reset")
                            || errorMsg.contains("你的主机中的软件中止了一个已建立的连接"))) {
                        return Flux.empty();
                    }
                    // 其他错误返回错误信息
                    return Flux.just("{\"error\":\"" + errorMsg + "\"}");
                });
    }
    
    /**
     * RAG增强的AI流式对话接口
     * 支持从知识库中检索相关信息增强回复
     * 
     * @param request RAG聊天请求
     * @param httpRequest HTTP请求（用于获取当前用户）
     * @return 流式响应
     */
    @PostMapping(value = "/chat-with-rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireMemberRole("使用RAG增强对话")
    public Flux<String> chatWithRag(
            @RequestBody RagChatRequest request,
            HttpServletRequest httpRequest) {
        logger.info("收到RAG增强对话请求");
        logger.debug("请求内容: 消息={}, 启用RAG={}", 
                    request.getMessage(), request.getUseRAG());
        
        try {
            // 从 JWT token 中获取当前用户ID
            Integer userId = (Integer) httpRequest.getAttribute("userId");
            
            if (userId == null) {
                logger.warn("RAG对话请求失败：无法获取用户ID");
                return Flux.just("{\"error\":\"用户未登录\"}");
            }
            
            logger.debug("获取到用户ID: {}", userId);
            
            // 调用RAG增强的聊天服务
            boolean useRAG = request.getUseRAG() != null ? request.getUseRAG() : true;
            boolean enableTools = request.getEnableTools() != null ? request.getEnableTools() : false;
            
            return aiChatService.chatWithRag(userId, request.getMessage(), useRAG, enableTools)
                    .doOnSubscribe(subscription -> logger.info("客户端开始订阅RAG流式响应，用户ID: {}", userId))
                    .doOnComplete(() -> logger.info("RAG流式对话完成，用户ID: {}", userId))
                    .doOnCancel(() -> logger.warn("客户端取消了RAG流式请求，用户ID: {}", userId))
                    .doOnError(error -> {
                        String errorMsg = error.getMessage();
                        if (errorMsg != null && (errorMsg.contains("ClientAbortException") 
                                || errorMsg.contains("Broken pipe")
                                || errorMsg.contains("Connection reset"))) {
                            logger.warn("客户端断开连接: {}", errorMsg);
                        } else {
                            logger.error("RAG流式对话错误: {}", errorMsg, error);
                        }
                    })
                    .onErrorResume(error -> {
                        String errorMsg = error.getMessage();
                        if (errorMsg != null && (errorMsg.contains("ClientAbortException") 
                                || errorMsg.contains("Broken pipe")
                                || errorMsg.contains("Connection reset"))) {
                            return Flux.empty();
                        }
                        return Flux.just("{\"error\":\"" + errorMsg + "\"}");
                    });
        } catch (Exception e) {
            logger.error("RAG对话请求处理失败: {}", e.getMessage(), e);
            return Flux.just("{\"error\":\"RAG对话失败: " + e.getMessage() + "\"}");
        }
    }

    /**
     * 获取当前用户的对话历史
     *
     * @param httpRequest HTTP请求（用于获取当前用户）
     * @return 对话历史列表
     */
    @GetMapping("/chat-history")
    @RequireMemberRole("查看对话历史")
    public ApiResponse<Map<String, Object>> getChatHistory(HttpServletRequest httpRequest) {
        logger.info("收到获取对话历史请求");

        try {
            // 从JWT token中获取当前用户ID
            Integer userId = (Integer) httpRequest.getAttribute("userId");

            if (userId == null) {
                logger.warn("获取对话历史失败：无法获取用户ID");
                return ApiResponse.error("用户未登录", ErrorCode.TOKEN_REQUIRED.getCode());
            }

            String conversationId = "user_" + userId;

            // 获取对话历史
            List<RedisChatMemory.ChatMessageRecord> history = chatMemory.getChatHistory(conversationId);

            // 获取统计信息
            RedisChatMemory.ChatHistoryStats stats = chatMemory.getStats(conversationId);

            Map<String, Object> result = new HashMap<>();
            result.put("history", history);
            result.put("stats", stats);

            logger.info("获取对话历史成功，用户ID: {}, 消息数: {}", userId, history.size());
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("获取对话历史失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取对话历史失败: " + e.getMessage(), ErrorCode.SYSTEM_ERROR.getCode());
        }
    }

    /**
     * 删除当前用户的对话历史
     *
     * @param httpRequest HTTP请求（用于获取当前用户）
     * @return 删除结果
     */
    @DeleteMapping("/chat-history")
    @RequireMemberRole("删除对话历史")
    public ApiResponse<Void> clearChatHistory(HttpServletRequest httpRequest) {
        logger.info("收到删除对话历史请求");

        try {
            // 从JWT token中获取当前用户ID
            Integer userId = (Integer) httpRequest.getAttribute("userId");

            if (userId == null) {
                logger.warn("删除对话历史失败：无法获取用户ID");
                return ApiResponse.error("用户未登录", ErrorCode.TOKEN_REQUIRED.getCode());
            }

            String conversationId = "user_" + userId;

            // 清除对话历史
            chatMemory.clear(conversationId);

            logger.info("删除对话历史成功，用户ID: {}", userId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            logger.error("删除对话历史失败: {}", e.getMessage(), e);
            return ApiResponse.error("删除对话历史失败: " + e.getMessage(), ErrorCode.SYSTEM_ERROR.getCode());
        }
    }
}