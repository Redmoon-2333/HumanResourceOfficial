package com.redmoon2333.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG初始化请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagInitRequest {
    
    /**
     * 知识库文件目录路径
     */
    private String sourcePath;
    
    /**
     * 是否强制重建索引（清空已有数据）
     */
    private Boolean forceReindex = false;
}
