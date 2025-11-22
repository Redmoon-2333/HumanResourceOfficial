package com.redmoon2333.controller;

import com.redmoon2333.annotation.RequireMemberRole;
import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.dto.ChatRequest;
import com.redmoon2333.dto.ChatResponse;
import com.redmoon2333.dto.PlanGeneratorRequest;
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

    /**
     * AI对话接口（带用户记忆）
     * 每个用户拥有独立的对话上下文和历史记忆
     *
     * @param request     聊天请求
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
        return aiChatService.generatePlanStream(request);
    }
}