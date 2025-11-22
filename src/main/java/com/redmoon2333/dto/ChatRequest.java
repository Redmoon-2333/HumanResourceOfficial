package com.redmoon2333.dto;

/**
 * AI聊天请求DTO
 */
public class ChatRequest {
    private String message;
    
    public ChatRequest() {}
    
    public ChatRequest(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
