package com.redmoon2333.controller;

import com.redmoon2333.annotation.RequireMinisterRole;
import com.redmoon2333.annotation.RequireMemberRole;
import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.dto.RagInitRequest;
import com.redmoon2333.dto.RagInitResponse;
import com.redmoon2333.dto.RagStatsResponse;
import com.redmoon2333.dto.RetrievedDocument;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.service.RagManagementService;
import com.redmoon2333.service.RagRetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * RAG管理控制器
 * 提供知识库初始化、统计等管理接口
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {
    
    private static final Logger logger = LoggerFactory.getLogger(RagController.class);
    
    @Autowired
    private RagManagementService ragManagementService;
    
    @Autowired
    private RagRetrievalService ragRetrievalService;
    
    /**
     * 初始化知识库
     * 
     * @param request 初始化请求
     * @return 初始化结果
     */
    @PostMapping("/initialize")
    @RequireMinisterRole("初始化知识库")
    public ApiResponse<RagInitResponse> initialize(@RequestBody RagInitRequest request) {
        logger.info("收到知识库初始化请求");
        
        try {
            if (request.getSourcePath() == null || request.getSourcePath().isEmpty()) {
                logger.warn("知识库路径为空，使用默认路径");
            }
            
            RagInitResponse response = ragManagementService.initializeKnowledgeBase(request);
            logger.info("知识库初始化成功");
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            logger.error("知识库初始化失败", e);
            return ApiResponse.error("初始化失败: " + e.getMessage(), 
                                    ErrorCode.SYSTEM_ERROR.getCode());
        }
    }
    
    /**
     * 获取知识库统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/stats")
    @RequireMemberRole("获取知识库统计")
    public ApiResponse<RagStatsResponse> getStats() {
        logger.info("收到获取统计信息请求");
        
        try {
            RagStatsResponse stats = ragManagementService.getStats();
            return ApiResponse.success(stats);
            
        } catch (Exception e) {
            logger.error("获取统计信息失败", e);
            return ApiResponse.error("获取统计信息失败: " + e.getMessage(), 
                                    ErrorCode.SYSTEM_ERROR.getCode());
        }
    }
    
    /**
     * 测试接口: 不设置相似度阈值的检索
     * 用于调试和验证数据库中是否有数据
     * 
     * @param query 查询文本
     * @param topK 返回文档数量（默认10）
     * @return 检索结果
     */
    @GetMapping("/test-retrieve")
    @RequireMemberRole("测试检索")
    public ApiResponse<?> testRetrieve(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int topK) {
        logger.info("收到测试检索请求: query={}, topK={}", query, topK);
        
        try {
            var results = ragRetrievalService.retrieveWithoutThreshold(query, topK);
            return ApiResponse.success(results);
            
        } catch (Exception e) {
            logger.error("测试检索失败", e);
            return ApiResponse.error("检索失败: " + e.getMessage(), 
                                    ErrorCode.SYSTEM_ERROR.getCode());
        }
    }
}
