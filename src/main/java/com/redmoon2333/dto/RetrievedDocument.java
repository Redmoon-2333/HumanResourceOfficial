package com.redmoon2333.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 检索到的文档DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedDocument {
    
    /**
     * 文档内容
     */
    private String content;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 相似度分数
     */
    private float score;
}
