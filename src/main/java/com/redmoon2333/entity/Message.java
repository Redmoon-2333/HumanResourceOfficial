package com.redmoon2333.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long messageId;
    private Integer receiverId;
    private Integer senderId;
    private String type;
    private String title;
    private String content;
    private String refType;
    private Long refId;
    private Boolean isRead;
    private LocalDateTime readTime;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
