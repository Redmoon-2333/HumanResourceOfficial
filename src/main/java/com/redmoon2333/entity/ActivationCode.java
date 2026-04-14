package com.redmoon2333.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.redmoon2333.enums.ActivationStatus;
import java.time.LocalDateTime;

@TableName(value = "activation_code", autoResultMap = true)
public class ActivationCode {
    @TableId(type = IdType.AUTO)
    private Integer codeId;

    private String code;

    private Integer creatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private ActivationStatus status = ActivationStatus.未使用;
    
    private Integer userId;
    
    private LocalDateTime useTime;
    
    private LocalDateTime expireTime;
    
    // 无参构造函数
    public ActivationCode() {
        // createTime 由 MyBatis-Plus 自动填充
    }
    
    // 带参数的构造函数
    public ActivationCode(String code, Integer creatorId, LocalDateTime expireTime) {
        this();
        this.code = code;
        this.creatorId = creatorId;
        this.expireTime = expireTime;
    }
    
    // Getters and Setters
    public Integer getCodeId() {
        return codeId;
    }
    
    public void setCodeId(Integer codeId) {
        this.codeId = codeId;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public Integer getCreatorId() {
        return creatorId;
    }
    
    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public ActivationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ActivationStatus status) {
        this.status = status;
    }
    
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getUseTime() {
        return useTime;
    }
    
    public void setUseTime(LocalDateTime useTime) {
        this.useTime = useTime;
    }
    
    public LocalDateTime getExpireTime() {
        return expireTime;
    }
    
    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
    
    @Override
    public String toString() {
        return "ActivationCode{" +
                "codeId=" + codeId +
                ", code='" + code + '\'' +
                ", creatorId=" + creatorId +
                ", createTime=" + createTime +
                ", status='" + status + '\'' +
                ", userId=" + userId +
                ", useTime=" + useTime +
                ", expireTime=" + expireTime +
                '}';
    }
}
