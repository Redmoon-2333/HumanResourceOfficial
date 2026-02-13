package com.redmoon2333.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 激活码响应DTO
 * 用于前后端数据交互，统一字段命名
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivationCodeResponse {

    /**
     * 激活码ID
     */
    private Integer id;

    /**
     * 激活码字符串
     */
    private String code;

    /**
     * 创建者ID
     */
    private Integer createdBy;

    /**
     * 创建者名称
     */
    private String creatorName;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 过期时间
     */
    private String expireTime;

    /**
     * 是否已使用
     */
    private Boolean used;

    /**
     * 使用者ID
     */
    private Integer usedBy;

    /**
     * 使用者名称
     */
    private String usedByName;

    /**
     * 使用时间
     */
    private String usedTime;
}
