package com.redmoon2333.entity;

import com.redmoon2333.enums.ActivationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activation_code")
public class ActivationCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_id")
    private Integer codeId;
    
    @Column(name = "code")
    private String code;
    
    @Column(name = "creator_id")
    private Integer creatorId;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ActivationStatus status = ActivationStatus.未使用;
    
    @Column(name = "user_id")
    private Integer userId;
    
    @Column(name = "use_time")
    private LocalDateTime useTime;
    
    @Column(name = "expire_time")
    private LocalDateTime expireTime;
    
    // 无参构造函数
    public ActivationCode() {
        this.createTime = LocalDateTime.now();
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