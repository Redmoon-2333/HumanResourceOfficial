package com.redmoon2333.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG统计信息响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagStatsResponse {
    
    /**
     * 文档总数
     */
    private int totalDocuments;
    
    /**
     * 向量总数
     */
    private long totalVectors;
    
    /**
     * 分类统计
     */
    private java.util.Map<String, Integer> categoryStats;
    
    /**
     * 最后更新时间
     */
    private String lastUpdateTime;
    
    /**
     * Collection名称
     */
    private String collectionName;
    
    /**
     * 向量维度
     */
    private int vectorDimension;
}
