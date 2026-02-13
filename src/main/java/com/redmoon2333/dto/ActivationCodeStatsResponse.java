package com.redmoon2333.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 激活码统计数据响应DTO
 * 用于返回激活码的汇总统计信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivationCodeStatsResponse {

    /**
     * 激活码总数
     */
    private Integer totalCount;

    /**
     * 未使用数量
     */
    private Integer unusedCount;

    /**
     * 已使用数量
     */
    private Integer usedCount;

    /**
     * 已过期数量
     */
    private Integer expiredCount;
}
