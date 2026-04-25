package com.redmoon2333.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.redmoon2333.entity.Message;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;

    public IPage<Message> getMessages(Integer userId, Integer page, Integer pageSize, String type, Boolean isRead) {
        Page<Message> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId);
        if (type != null && !type.isBlank()) {
            wrapper.eq(Message::getType, type);
        }
        if (isRead != null) {
            wrapper.eq(Message::getIsRead, isRead);
        }
        wrapper.orderByDesc(Message::getCreateTime);
        return messageMapper.selectPage(pageParam, wrapper);
    }

    public Map<String, Integer> getUnreadCount(Integer userId) {
        // 查询所有未读消息，在内存中按类型分组统计
        // 注意：如果未读消息量很大（>1000），应考虑用 Redis 缓存计数
        List<Message> unreadMessages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getReceiverId, userId)
                        .eq(Message::getIsRead, false)
                        .select(Message::getType));

        Map<String, Integer> byType = new HashMap<>();
        for (Message msg : unreadMessages) {
            byType.merge(msg.getType(), 1, Integer::sum);
        }

        Map<String, Integer> result = new HashMap<>(byType);
        result.put("unreadCount", byType.values().stream().mapToInt(Integer::intValue).sum());
        return result;
    }

    @Transactional
    public void markAsRead(Long messageId, Integer userId) {
        Message msg = messageMapper.selectById(messageId);
        if (msg == null) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND);
        }
        if (!msg.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_OWNED);
        }
        if (!msg.getIsRead()) {
            msg.setIsRead(true);
            msg.setReadTime(LocalDateTime.now());
            messageMapper.updateById(msg);
        }
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        // 批量更新：一条 SQL 更新所有未读消息，避免循环内逐条更新
        messageMapper.update(null,
                new LambdaUpdateWrapper<Message>()
                        .eq(Message::getReceiverId, userId)
                        .eq(Message::getIsRead, false)
                        .set(Message::getIsRead, true)
                        .set(Message::getReadTime, LocalDateTime.now()));
    }

    @Transactional
    public void deleteMessage(Long messageId, Integer userId) {
        Message msg = messageMapper.selectById(messageId);
        if (msg == null) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND);
        }
        if (!msg.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_OWNED);
        }
        messageMapper.deleteById(messageId);
    }

    @Transactional
    public Message sendMessage(Integer senderId, Integer receiverId, String title, String content, String type) {
        if (receiverId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "收件人不能为空");
        }
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "内容不能为空");
        }
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type != null ? type : "站内信");
        message.setIsRead(false);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }
}
