package com.redmoon2333.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG聊天请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagChatRequest {
    
    /**
     * 用户消息
     */
    private String message;
    
    /**
     * 是否启用RAG检索（默认true）
     */
    private Boolean useRAG = true;
    
    /**
     * 是否启用Tool Calling（默认true）
     */
    private Boolean enableTools = true;
}
