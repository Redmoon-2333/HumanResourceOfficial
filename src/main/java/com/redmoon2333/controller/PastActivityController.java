package com.redmoon2333.controller;

import com.redmoon2333.annotation.RequireMinisterRole;
import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.dto.PageResponse;
import com.redmoon2333.dto.PastActivityRequest;
import com.redmoon2333.dto.PastActivityResponse;
import com.redmoon2333.service.PastActivityService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 往届活动控制器
 */
@RestController
@RequestMapping("/api/past-activities")
public class PastActivityController {
    
    private static final Logger logger = LoggerFactory.getLogger(PastActivityController.class);
    
    @Autowired
    private PastActivityService pastActivityService;
    
    /**
     * 分页查询往届活动
     * @param pageNum 页码，默认1
     * @param pageSize 每页大小，默认10
     * @param year 年份筛选（可选）
     * @param title 活动名称搜索（可选）
     * @return 分页结果
     */
    @GetMapping
    public ApiResponse<PageResponse<PastActivityResponse>> getPagedPastActivities(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String title) {
        
        PageResponse<PastActivityResponse> result = pastActivityService.getPagedPastActivities(
                pageNum, pageSize, year, title);
        
        return ApiResponse.success(result);
    }
    
    /**
     * 根据ID查询往届活动详情
     * @param id 往届活动ID
     * @return 往届活动详情
     */
    @GetMapping("/{id}")
    public ApiResponse<PastActivityResponse> getPastActivityById(@PathVariable Integer id) {
        
        PastActivityResponse result = pastActivityService.getPastActivityById(id);
        
        return ApiResponse.success(result);
    }
    
    /**
     * 创建往届活动（需要部长权限）
     * @param request 往届活动请求
     * @return 创建的往届活动
     */
    @PostMapping
    @RequireMinisterRole
    public ApiResponse<PastActivityResponse> createPastActivity(@Valid @RequestBody PastActivityRequest request) {
        logger.info("创建往届活动 - 标题: {}", request.getTitle());
        
        PastActivityResponse result = pastActivityService.createPastActivity(request);
        
        return ApiResponse.success("往届活动创建成功", result);
    }
    
    /**
     * 更新往届活动（需要部长权限）
     * @param id 往届活动ID
     * @param request 更新请求
     * @return 更新后的往届活动
     */
    @PutMapping("/{id}")
    @RequireMinisterRole
    public ApiResponse<PastActivityResponse> updatePastActivity(@PathVariable Integer id, 
                                                              @Valid @RequestBody PastActivityRequest request) {
        logger.info("更新往届活动 - ID: {}", id);
        
        PastActivityResponse result = pastActivityService.updatePastActivity(id, request);
        
        return ApiResponse.success("往届活动更新成功", result);
    }
    
    /**
     * 删除往届活动（需要部长权限）
     * @param id 往届活动ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @RequireMinisterRole
    public ApiResponse<String> deletePastActivity(@PathVariable Integer id) {
        logger.info("删除往届活动 - ID: {}", id);
        
        pastActivityService.deletePastActivity(id);
        
        return ApiResponse.success("往届活动删除成功");
    }
    
    /**
     * 获取所有年份列表
     * @return 年份列表
     */
    @GetMapping("/years")
    public ApiResponse<List<Integer>> getAllYears() {
        
        List<Integer> result = pastActivityService.getAllYears();
        
        return ApiResponse.success(result);
    }
    
    /**
     * 根据年份统计活动数量
     * @param year 年份
     * @return 活动数量
     */
    @GetMapping("/years/{year}/count")
    public ApiResponse<Integer> countByYear(@PathVariable Integer year) {
        
        int count = pastActivityService.countByYear(year);
        
        return ApiResponse.success(count);
    }
}