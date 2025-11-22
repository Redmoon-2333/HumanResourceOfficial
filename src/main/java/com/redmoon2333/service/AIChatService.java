package com.redmoon2333.service;

import com.redmoon2333.dto.ChatResponse;
import com.redmoon2333.dto.PlanGeneratorRequest;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * AI聊天服务
 * 提供基于用户的对话记忆存储和策划案生成功能
 */
@Service
public class AIChatService {
    private static final Logger logger = LoggerFactory.getLogger(AIChatService.class);
    
    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;
    
    @Value("classpath:/prompttemplate/plangenerator.txt")
    private org.springframework.core.io.Resource planTemplate;
    
    /**
     * 发送聊天消息（带用户记忆）
     * 
     * @param userId 用户ID，用于隔离不同用户的对话记忆
     * @param message 用户消息
     * @return 聊天响应
     */
    public ChatResponse chat(Integer userId, String message) {
        logger.info("用户 {} 发送消息: {}", userId, message);
        
        // 使用用户ID作为会话ID，确保每个用户有独立的对话记忆
        String conversationId = "user_" + userId;
        
        logger.debug("为用户 {} 创建会话ID: {}", userId, conversationId);
        
        try {
            String response = qwenChatClient
                    .prompt(message)
                    .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, conversationId))
                    .call()
                    .content();
            
            logger.info("AI响应用户 {}: {}", userId, response);
            logger.debug("会话 {} 的响应内容长度: {} 字符", conversationId, response.length());
            
            return new ChatResponse(response, conversationId);
        } catch (Exception e) {
            logger.error("AI对话处理失败，用户ID: {}, 消息: {}", userId, message, e);
            throw new RuntimeException("AI对话处理失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成活动策划案
     * 
     * @param request 策划案请求参数
     * @return HTML格式的策划案
     */
    public String generatePlan(PlanGeneratorRequest request) {
        logger.info("开始生成策划案，主题: {}", request.getTheme());
        logger.debug("策划案请求参数详情: 主题={}, 主办方={}, 时间={}, 地点={}, 工作人员={}, 参与人数={}, 活动目的={}, 部长人数={}, 部员人数={}",
                request.getTheme(), request.getOrganizer(), request.getEventTime(), request.getEventLocation(),
                request.getStaff(), request.getParticipants(), request.getPurpose(), request.getLeaderCount(),
                request.getMemberCount());
        
        try {
            PromptTemplate promptTemplate = new PromptTemplate(planTemplate);
            
            // 构建模板参数
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
            
            logger.debug("构建提示词模板参数完成");
            
            Prompt prompt = promptTemplate.create(params);
            logger.debug("创建提示词完成，提示词内容长度: {} 字符", prompt.getContents().length());
            
            String planContent = qwenChatClient.prompt(prompt).call().content();
            
            logger.info("策划案生成成功，主题: {}", request.getTheme());
            logger.debug("生成的策划案内容长度: {} 字符", planContent.length());
            
            return planContent;
        } catch (Exception e) {
            logger.error("生成策划案失败，请求参数: {}", request, e);
            throw new RuntimeException("生成策划案失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 流式生成活动策划案
     * 
     * @param request 策划案请求参数
     * @return HTML格式的策划案流
     */
    public Flux<String> generatePlanStream(PlanGeneratorRequest request) {
        logger.info("开始流式生成策划案，主题: {}", request.getTheme());
        
        try {
            PromptTemplate promptTemplate = new PromptTemplate(planTemplate);
            
            // 构建模板参数
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
            
            Prompt prompt = promptTemplate.create(params);
            
            // 使用stream()方法获取流式响应，直接返回content内容
            return qwenChatClient.prompt(prompt).stream().content();
        } catch (Exception e) {
            logger.error("流式生成策划案失败: {}", e.getMessage(), e);
            return Flux.error(new RuntimeException("流式生成策划案失败: " + e.getMessage(), e));
        }
    }
}