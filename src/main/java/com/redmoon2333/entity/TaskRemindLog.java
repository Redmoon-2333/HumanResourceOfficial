package com.redmoon2333.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_remind_log")
public class TaskRemindLog {
    @TableId(type = IdType.AUTO)
    private Long logId;
    private Long assignmentId;
    private Integer operatorId;
    private LocalDateTime remindTime;
    private String channel;
}
