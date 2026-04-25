package com.redmoon2333.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.redmoon2333.annotation.RequireMemberRole;
import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.entity.Message;
import com.redmoon2333.entity.User;
import com.redmoon2333.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ApiResponse<IPage<Message>> getMessages(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead) {
        Integer userId = (Integer) request.getAttribute("userId");
        IPage<Message> result = messageService.getMessages(userId, page, pageSize, type, isRead);
        return ApiResponse.success(result);
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Integer>> getUnreadCount(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        Map<String, Integer> result = messageService.getUnreadCount(userId);
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(HttpServletRequest request, @PathVariable Long id) {
        Integer userId = (Integer) request.getAttribute("userId");
        messageService.markAsRead(id, userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        messageService.markAllAsRead(userId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMessage(HttpServletRequest request, @PathVariable Long id) {
        Integer userId = (Integer) request.getAttribute("userId");
        messageService.deleteMessage(id, userId);
        return ApiResponse.success(null);
    }

    @RequireMemberRole("发送站内信")
    @PostMapping
    public ApiResponse<Message> sendMessage(
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Integer senderId = (Integer) request.getAttribute("userId");
        Integer receiverId = body.get("receiverId") != null
                ? ((Number) body.get("receiverId")).intValue() : null;
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        String type = (String) body.get("type");
        Message message = messageService.sendMessage(senderId, receiverId, title, content, type);
        return ApiResponse.success(message);
    }
}
