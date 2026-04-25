package com.redmoon2333.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("role_change_log")
public class RoleChangeLog {
    @TableId(type = IdType.AUTO)
    private Long logId;
    private Integer targetUserId;
    private Integer operatorId;
    private String beforeRoleHistory;
    private String afterRoleHistory;
    private String reason;
    private LocalDateTime changeTime;
}
