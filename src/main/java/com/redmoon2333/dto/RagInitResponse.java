package com.redmoon2333.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG初始化响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagInitResponse {
    
    /**
     * 总文件数
     */
    private int totalFiles;
    
    /**
     * 成功处理的文件数
     */
    private int processedFiles;
    
    /**
     * 失败的文件数
     */
    private int failedFiles;
    
    /**
     * 总分块数
     */
    private int totalChunks;
    
    /**
     * 新增的分块数
     */
    private int newChunks;
    
    /**
     * 重复跳过的分块数
     */
    private int duplicateChunks;
    
    /**
     * 错误信息列表
     */
    private List<FileError> errors = new ArrayList<>();
    
    /**
     * 文件错误信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileError {
        /**
         * 文件名
         */
        private String fileName;
        
        /**
         * 错误原因
         */
        private String reason;
    }
}
