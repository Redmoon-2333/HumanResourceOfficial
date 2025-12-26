package com.redmoon2333.service;

import com.redmoon2333.dto.ChatResponse;
import com.redmoon2333.dto.PlanGeneratorRequest;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * AI聊天服务
 * 提供基于用户的对话记忆存储和策划案生成功能
 */
@Service
public class AIChatService {
    private static final Logger logger = LoggerFactory.getLogger(AIChatService.class);
    
    // 系统提示词：定义AI的身份、职责和输出格式要求
    private static final String SYSTEM_PROMPT = """
            你是人力资源中心的小助理。你的主要职责是帮助部员解决颇痒（活动组织、团队协作、部门流程、新人培训）以及关心部员学习生活（学习方法、暴力情绪、职业规划）。
            你的回答风格应该是友好、亲切、专业而带温度，并指需具体可行的建议。
            
            格式要求（必须严格遵守）：
            1. 使用Markdown语法：##标题、-列表、**加粗**
            2. 绝对禁止使用空行！所有元素之间只用一个\\n分隔
            3. ##标题和内容之间：只用一个\\n，不要\\n\\n
            4. 列表项之间：只用一个\\n，不要\\n\\n
            5. 段落之间：只用一个\\n，不要\\n\\n
            6. 不要使用---、>、```等元素
            7. 输出示例："##标题\\n内容\\n-项目1\\n-项目2"
            """;
    
    @Resource(name = "qwenChatClient")
    private ChatClient qwenChatClient;
    
    @Value("classpath:/prompttemplate/plangenerator.txt")
    private org.springframework.core.io.Resource planTemplate;
    
    @Autowired(required = false)
    private RagRetrievalService ragRetrievalService;
    
    @Autowired(required = false)
    private ToolService toolService;
    
    /**
     * 清理AI返回内容中的多余空行
     * 将所有2个或更多连续的\n替换为1个\n
     */
    private String cleanExtraNewlines(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        // 将所有连续的多个换行符替换为一个
        String cleaned = content.replaceAll("\n{2,}", "\n");
        logger.debug("清理前长度: {}, 清理后长度: {}", content.length(), cleaned.length());
        return cleaned;
    }
    
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
                    .prompt()
                    .system(SYSTEM_PROMPT)  // 添加系统提示词
                    .user(message)
                    .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, conversationId))
                    .call()
                    .content();
            
            // 清理多余换行
            response = cleanExtraNewlines(response);
            
            logger.info("AI响应用户 {}: {}", userId, response);
            logger.debug("会话 {} 的响应内容长度: {} 字符", conversationId, response.length());
            
            return new ChatResponse(response, conversationId);
        } catch (Exception e) {
            logger.error("AI对话处理失败，用户ID: {}, 消息: {}", userId, message, e);
            throw new RuntimeException("AI对话处理失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 流式发送聊天消息（带用户记忆）
     * 
     * @param userId 用户ID，用于隔离不同用户的对话记忆
     * @param message 用户消息
     * @return 流式响应
     */
    public Flux<String> chatStream(Integer userId, String message) {
        logger.info("用户 {} 发送流式消息: {}", userId, message);
        
        // 使用用户ID作为会话ID，确保每个用户有独立的对话记忆
        String conversationId = "user_" + userId;
        
        logger.debug("为用户 {} 创建会话ID: {}", userId, conversationId);
        
        try {
            return qwenChatClient
                    .prompt()
                    .system(SYSTEM_PROMPT)  // 添加系统提示词
                    .user(message)
                    .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, conversationId))
                    .stream()
                    .content()
                    .doOnNext(chunk -> logger.debug("发送数据块: {} 字符", chunk.length()))
                    .doOnComplete(() -> logger.info("流式对话完成，用户ID: {}", userId))
                    .doOnError(error -> {
                        String errorMsg = error.getMessage();
                        if (errorMsg != null && (errorMsg.contains("ClientAbortException") 
                                || errorMsg.contains("Broken pipe")
                                || errorMsg.contains("Connection reset")
                                || errorMsg.contains("你的主机中的软件中止了一个已建立的连接"))) {
                            logger.warn("客户端提前断开连接，用户ID: {}", userId);
                        } else {
                            logger.error("流式对话错误，用户ID: {}, 错误: {}", userId, errorMsg);
                        }
                    })
                    .onErrorResume(error -> {
                        String errorMsg = error.getMessage();
                        if (errorMsg != null && (errorMsg.contains("ClientAbortException") 
                                || errorMsg.contains("Broken pipe")
                                || errorMsg.contains("Connection reset")
                                || errorMsg.contains("你的主机中的软件中止了一个已建立的连接"))) {
                            return Flux.empty();
                        }
                        return Flux.error(error);
                    });
        } catch (Exception e) {
            logger.error("流式AI对话处理失败: {}", e.getMessage(), e);
            return Flux.error(new RuntimeException("流式AI对话处理失败: " + e.getMessage(), e));
        }
    }
    
    /**
     * RAG增强的聊天功能（流式）
     * 
     * @param userId 用户ID
     * @param message 用户消息
     * @param useRAG 是否启用RAG检索
     * @return 流式响应
     */
    public Flux<String> chatWithRag(Integer userId, String message, boolean useRAG) {
        return chatWithRag(userId, message, useRAG, false);
    }
    
    /**
     * RAG增强的聊天功能（流式，支持Tool Calling）
     * 
     * @param userId 用户ID
     * @param message 用户消息
     * @param useRAG 是否启用RAG检索
     * @param enableTools 是否启用Tool Calling
     * @return 流式响应
     */
    public Flux<String> chatWithRag(Integer userId, String message, boolean useRAG, boolean enableTools) {
        logger.info("用户 {} 发送RAG增强消息: {}, 启用RAG: {}, 启用工具: {}", 
                   userId, message, useRAG, enableTools);
        
        // 如果不启用RAG或RAG服务不可用，降级到普通聊天
        if (!useRAG || ragRetrievalService == null) {
            logger.debug("RAG未启用或服务不可用，使用普通聊天模式");
            return chatStream(userId, message);
        }
        
        try {
            // 1. 检索相关文档
            List<RagRetrievalService.RetrievedDocument> documents = 
                ragRetrievalService.retrieve(message);
            
            // 2. 格式化上下文
            String context = ragRetrievalService.formatContext(documents);
            
            // 3. 构建增强的系统提示词
            String enhancedSystemPrompt = SYSTEM_PROMPT + "\n\n" + context;
            
            // 4. 如果启用工具，添加工具说明
            if (enableTools && toolService != null) {
                enhancedSystemPrompt += "\n\n你可以调用以下工具查询数据库：\n" +
                    "1. searchDepartmentMembers(name): 根据姓名搜索部门成员\n" +
                    "2. getAlumniByYear(year): 获取指定年份的往届成员\n" +
                    "3. getDepartmentStats(): 获取部门统计信息\n" +
                    "当用户问到相关问题时，你应该主动调用这些工具。";
            }
            
            logger.debug("检索到 {} 个相关文档，上下文长度: {} 字符", 
                        documents.size(), context.length());
            
            // 5. 处理可能的工具调用
            String processedMessage = message;
            if (enableTools && toolService != null) {
                processedMessage = processToolCalls(message);
            }
            
            // 6. 调用AI生成回复
            String conversationId = "user_" + userId;
            
            return qwenChatClient
                    .prompt()
                    .system(enhancedSystemPrompt)
                    .user(processedMessage)
                    .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, conversationId))
                    .stream()
                    .content()
                    .doOnNext(chunk -> logger.trace("发送RAG数据块: {} 字符", chunk.length()))
                    .doOnComplete(() -> logger.info("RAG流式对话完成，用户ID: {}", userId))
                    .doOnError(error -> {
                        String errorMsg = error.getMessage();
                        if (errorMsg != null && (errorMsg.contains("ClientAbortException") 
                                || errorMsg.contains("Broken pipe")
                                || errorMsg.contains("Connection reset"))) {
                            logger.warn("客户端提前断开连接，用户ID: {}", userId);
                        } else {
                            logger.error("RAG流式对话错误，用户ID: {}, 错误: {}", userId, errorMsg);
                        }
                    })
                    .onErrorResume(error -> {
                        String errorMsg = error.getMessage();
                        if (errorMsg != null && (errorMsg.contains("ClientAbortException") 
                                || errorMsg.contains("Broken pipe")
                                || errorMsg.contains("Connection reset"))) {
                            return Flux.empty();
                        }
                        return Flux.error(error);
                    });
                    
        } catch (Exception e) {
            logger.error("RAG检索失败，降级到普通聊天: {}", e.getMessage(), e);
            // 降级到普通聊天
            return chatStream(userId, message);
        }
    }
    
    /**
     * 处理工具调用
     * 检测用户消息中是否需要调用工具，如果需要则执行并返回结果
     * 
     * @param message 用户消息
     * @return 处理后的消息
     */
    private String processToolCalls(String message) {
        String lowerMessage = message.toLowerCase();
        StringBuilder result = new StringBuilder();
        result.append(message);
        
        try {
            // 检测是否需要搜索成员
            if (lowerMessage.contains("搜索") || lowerMessage.contains("查找") || 
                lowerMessage.contains("查询") && (lowerMessage.contains("成员") || lowerMessage.contains("人"))) {
                
                // 提取姓名（简单的实现）
                String[] words = message.split("[、，\\s]+");
                for (String word : words) {
                    if (word.length() >= 2 && word.length() <= 4 && 
                        !word.contains("搜") && !word.contains("查") && 
                        !word.contains("成员") && !word.contains("人")) {
                        
                        String toolResult = toolService.searchDepartmentMembers(word);
                        result.append("\n\n[查询结果]\n").append(toolResult);
                        logger.info("执行工具调用: searchDepartmentMembers({})", word);
                        break;
                    }
                }
            }
            
            // 检测是否需要查询往届成员
            if (lowerMessage.contains("往届") || lowerMessage.contains("历史")) {
                // 提取年份
                Integer year = null;
                if (message.matches(".*\\d{4}.*")) {
                    String yearStr = message.replaceAll(".*?(\\d{4}).*", "$1");
                    try {
                        year = Integer.parseInt(yearStr);
                    } catch (NumberFormatException e) {
                        // 忽略
                    }
                }
                
                String toolResult = toolService.getAlumniByYear(year);
                result.append("\n\n[查询结果]\n").append(toolResult);
                logger.info("执行工具调用: getAlumniByYear({})", year);
            }
            
            // 检测是否需要统计信息
            if (lowerMessage.contains("统计") || lowerMessage.contains("总共") || 
                lowerMessage.contains("多少人") || lowerMessage.contains("多少届")) {
                
                String toolResult = toolService.getDepartmentStats();
                result.append("\n\n[查询结果]\n").append(toolResult);
                logger.info("执行工具调用: getDepartmentStats()");
            }
            
        } catch (Exception e) {
            logger.error("工具调用失败", e);
        }
        
        return result.toString();
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
            // 添加错误处理，捕获客户端断开连接的异常
            return qwenChatClient.prompt(prompt).stream().content()
                    .doOnNext(chunk -> logger.debug("发送数据块: {} 字符", chunk.length()))
                    .doOnComplete(() -> logger.info("流式策划案生成完成，主题: {}", request.getTheme()))
                    .doOnError(error -> {
                        // 判断是否为客户端断开连接的异常
                        String errorMsg = error.getMessage();
                        if (errorMsg != null && (errorMsg.contains("ClientAbortException") 
                                || errorMsg.contains("Broken pipe")
                                || errorMsg.contains("Connection reset")
                                || errorMsg.contains("你的主机中的软件中止了一个已建立的连接"))) {
                            logger.warn("客户端提前断开连接，主题: {}", request.getTheme());
                        } else {
                            logger.error("流式策划案生成错误，主题: {}, 错误: {}", request.getTheme(), errorMsg);
                        }
                    })
                    // 忽略客户端断开连接的异常，避免后端日志大量报错
                    .onErrorResume(error -> {
                        String errorMsg = error.getMessage();
                        if (errorMsg != null && (errorMsg.contains("ClientAbortException") 
                                || errorMsg.contains("Broken pipe")
                                || errorMsg.contains("Connection reset")
                                || errorMsg.contains("你的主机中的软件中止了一个已建立的连接"))) {
                            // 客户端断开是正常情况，返回空流
                            return Flux.empty();
                        }
                        // 其他错误正常抛出
                        return Flux.error(error);
                    });
        } catch (Exception e) {
            logger.error("流式生成策划案失败: {}", e.getMessage(), e);
            return Flux.error(new RuntimeException("流式生成策划案失败: " + e.getMessage(), e));
        }
    }
}