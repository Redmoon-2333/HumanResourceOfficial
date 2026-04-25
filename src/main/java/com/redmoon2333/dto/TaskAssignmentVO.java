package com.redmoon2333.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskAssignmentVO {
    private Long assignmentId;
    private Long taskId;
    private Integer assigneeId;
    private String status;
    private LocalDateTime doneTime;
    private String doneRemark;
    private LocalDateTime lastRemindTime;
    private Integer remindCount;
    private LocalDateTime createTime;

    private String taskTitle;
    private String taskDescription;
    private LocalDateTime taskDueTime;
    private Integer taskPriority;
    private String taskStatus;
    private Integer creatorId;
    private String creatorName;
}