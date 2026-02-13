package com.redmoon2333.controller;

import com.redmoon2333.annotation.RequireMinisterRole;
import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.service.PerformanceMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 性能监控控制器
 * 提供性能指标查看接口
 */
@RestController
@RequestMapping("/api/performance")
public class PerformanceController {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceController.class);
    
    @Autowired
    private PerformanceMonitorService performanceMonitorService;
    
    /**
     * 获取性能报告
     * 需要部长权限
     */
    @GetMapping("/report")
    @RequireMinisterRole("查看性能报告")
    public ApiResponse<PerformanceMonitorService.PerformanceReport> getReport() {
        logger.info("获取性能报告");
        try {
            PerformanceMonitorService.PerformanceReport report = performanceMonitorService.getReport();
            logger.debug("性能报告: {}", report);
            return ApiResponse.success(report);
        } catch (Exception e) {
            logger.error("获取性能报告失败", e);
            return ApiResponse.error("获取性能报告失败: " + e.getMessage(), 500);
        }
    }
    
    /**
     * 重置性能统计数据
     * 需要部长权限
     */
    @PostMapping("/reset")
    @RequireMinisterRole("重置性能统计")
    public ApiResponse<String> resetStats() {
        logger.info("重置性能统计数据");
        try {
            performanceMonitorService.reset();
            return ApiResponse.success("性能统计数据已重置");
        } catch (Exception e) {
            logger.error("重置性能统计失败", e);
            return ApiResponse.error("重置失败: " + e.getMessage(), 500);
        }
    }
}
