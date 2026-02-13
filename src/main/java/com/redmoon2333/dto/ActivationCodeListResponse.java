package com.redmoon2333.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 激活码列表响应DTO
 * 包含激活码列表和统计数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivationCodeListResponse {

    /**
     * 激活码列表
     */
    private List<ActivationCodeResponse> codes;

    /**
     * 统计数据
     */
    private ActivationCodeStatsResponse stats;
}
