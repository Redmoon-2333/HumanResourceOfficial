package com.redmoon2333.dto;

/**
 * AI聊天响应DTO
 */
public class ChatResponse {
    private String response;
    private String conversationId;
    
    public ChatResponse() {}
    
    public ChatResponse(String response, String conversationId) {
        this.response = response;
        this.conversationId = conversationId;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
