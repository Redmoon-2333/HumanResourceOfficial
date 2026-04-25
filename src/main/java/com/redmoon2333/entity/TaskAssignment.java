package com.redmoon2333.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_assignment")
public class TaskAssignment {
    @TableId(type = IdType.AUTO)
    private Long assignmentId;
    private Long taskId;
    private Integer assigneeId;
    private String status;
    private LocalDateTime doneTime;
    private String doneRemark;
    private LocalDateTime lastRemindTime;
    private Integer remindCount;
    private LocalDateTime createTime;
}
