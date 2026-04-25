package com.redmoon2333.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long taskId;
    private String title;
    private String description;
    private Integer creatorId;
    private Integer creatorYear;
    private Integer targetYear;
    private LocalDateTime dueTime;
    private Integer priority;
    private String status;
    private Integer remindCooldownMinutes;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
