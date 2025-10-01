package com.redmoon2333.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.redmoon2333.dto.PageResponse;
import com.redmoon2333.dto.PastActivityRequest;
import com.redmoon2333.dto.PastActivityResponse;
import com.redmoon2333.entity.PastActivity;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.mapper.PastActivityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 往届活动服务
 */
@Service
@Transactional
public class PastActivityService {
    
    private static final Logger logger = LoggerFactory.getLogger(PastActivityService.class);
    
    @Autowired
    private PastActivityMapper pastActivityMapper;
    
    /**
     * 分页查询往届活动
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param year 年份（可选）
     * @param title 活动名称（可选，模糊搜索）
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    public PageResponse<PastActivityResponse> getPagedPastActivities(int pageNum, int pageSize, 
                                                                   Integer year, String title) {
        
        try {
            // 参数校验
            if (pageNum <= 0 || pageSize <= 0) {
                logger.warn("分页参数非法 - 页码: {}, 每页大小: {}", pageNum, pageSize);
                throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "分页参数必须大于0");
            }
            
            if (pageSize > 100) {
                logger.warn("每页大小超过限制 - 请求大小: {}", pageSize);
                throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "每页最多100条记录");
            }
            
            PageHelper.startPage(pageNum, pageSize);
            
            List<PastActivity> pastActivities;
            
            // 根据条件查询
            if (year != null && StringUtils.hasText(title)) {
                pastActivities = pastActivityMapper.findByYearAndTitleLike(year, title.trim());
            } else if (year != null) {
                pastActivities = pastActivityMapper.findByYear(year);
            } else if (StringUtils.hasText(title)) {
                pastActivities = pastActivityMapper.findByTitleLike(title.trim());
            } else {
                pastActivities = pastActivityMapper.findAll();
            }
            
            PageInfo<PastActivity> pageInfo = new PageInfo<>(pastActivities);
            List<PastActivityResponse> responses = PastActivityResponse.fromList(pastActivities);
            
            return PageResponse.of(
                responses, pageInfo.getPageNum(), pageInfo.getPageSize(), pageInfo.getTotal());
            
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            logger.error("分页查询往届活动数据访问异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据查询失败", e);
        } catch (Exception e) {
            logger.error("分页查询往届活动未知异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统内部错误", e);
        }
    }
    
    /**
     * 根据ID查询往届活动
     * @param pastActivityId 往届活动ID
     * @return 往届活动详情
     */
    @Transactional(readOnly = true)
    public PastActivityResponse getPastActivityById(Integer pastActivityId) {
        
        try {
            // 参数校验
            if (pastActivityId == null || pastActivityId <= 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "往届活动ID不能为空且必须大于0");
            }
            
            PastActivity pastActivity = pastActivityMapper.findById(pastActivityId);
            if (pastActivity == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "往届活动不存在");
            }
            
            return PastActivityResponse.from(pastActivity);
            
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            logger.error("查询往届活动数据访问异常 - ID: {}", pastActivityId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据查询失败", e);
        } catch (Exception e) {
            logger.error("查询往届活动未知异常 - ID: {}", pastActivityId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统内部错误", e);
        }
    }
    
    /**
     * 创建往届活动
     * @param request 往届活动请求
     * @return 创建的往届活动
     */
    public PastActivityResponse createPastActivity(PastActivityRequest request) {
        logger.info("创建往届活动 - 标题: {}", request.getTitle());
        
        try {
            // 参数校验
            if (request == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "请求参数不能为空");
            }
            
            // 业务校验：检查年份是否合理
            int currentYear = LocalDateTime.now().getYear();
            if (request.getYear() != null && request.getYear() > currentYear) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "年份不能大于当前年份");
            }
            
            PastActivity pastActivity = new PastActivity();
            pastActivity.setTitle(request.getTitle());
            pastActivity.setCoverImage(request.getCoverImage());
            pastActivity.setPushUrl(request.getPushUrl());
            pastActivity.setYear(request.getYear());
            pastActivity.setCreateTime(LocalDateTime.now());
            
            int result = pastActivityMapper.insert(pastActivity);
            if (result <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建往届活动失败");
            }
            
            logger.info("创建往届活动成功 - ID: {}", pastActivity.getPastActivityId());
            return PastActivityResponse.from(pastActivity);
            
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            logger.error("创建往届活动数据访问异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据保存失败", e);
        } catch (Exception e) {
            logger.error("创建往届活动未知异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统内部错误", e);
        }
    }
    
    /**
     * 更新往届活动
     * @param pastActivityId 往届活动ID
     * @param request 更新请求
     * @return 更新后的往届活动
     */
    public PastActivityResponse updatePastActivity(Integer pastActivityId, PastActivityRequest request) {
        logger.info("更新往届活动 - ID: {}", pastActivityId);
        
        try {
            // 参数校验
            if (pastActivityId == null || pastActivityId <= 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "往届活动ID不能为空且必须大于0");
            }
            
            if (request == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "请求参数不能为空");
            }
            
            PastActivity existingActivity = pastActivityMapper.findById(pastActivityId);
            if (existingActivity == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "往届活动不存在");
            }
            
            // 业务校验：检查年份是否合理
            int currentYear = LocalDateTime.now().getYear();
            if (request.getYear() != null && request.getYear() > currentYear) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "年份不能大于当前年份");
            }
            
            // 更新字段
            existingActivity.setTitle(request.getTitle());
            existingActivity.setCoverImage(request.getCoverImage());
            existingActivity.setPushUrl(request.getPushUrl());
            existingActivity.setYear(request.getYear());
            
            int result = pastActivityMapper.update(existingActivity);
            if (result <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新往届活动失败");
            }
            
            logger.info("更新往届活动成功 - ID: {}", pastActivityId);
            return PastActivityResponse.from(existingActivity);
            
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            logger.error("更新往届活动数据访问异常 - ID: {}", pastActivityId, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据更新失败", e);
        } catch (Exception e) {
            logger.error("更新往届活动未知异常 - ID: {}", pastActivityId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统内部错误", e);
        }
    }
    
    /**
     * 删除往届活动
     * @param pastActivityId 往届活动ID
     */
    public void deletePastActivity(Integer pastActivityId) {
        logger.info("删除往届活动 - ID: {}", pastActivityId);
        
        try {
            // 参数校验
            if (pastActivityId == null || pastActivityId <= 0) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "往届活动ID不能为空且必须大于0");
            }
            
            PastActivity existingActivity = pastActivityMapper.findById(pastActivityId);
            if (existingActivity == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "往届活动不存在");
            }
            
            int result = pastActivityMapper.deleteById(pastActivityId);
            if (result <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除往届活动失败");
            }
            
            logger.info("删除往届活动成功 - ID: {}", pastActivityId);
            
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            logger.error("删除往届活动数据访问异常 - ID: {}", pastActivityId, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "数据删除失败", e);
        } catch (Exception e) {
            logger.error("删除往届活动未知异常 - ID: {}", pastActivityId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统内部错误", e);
        }
    }
    
    /**
     * 获取所有年份列表
     * @return 年份列表
     */
    @Transactional(readOnly = true)
    public List<Integer> getAllYears() {
        
        try {
            return pastActivityMapper.findDistinctYears();
            
        } catch (DataAccessException e) {
            logger.error("查询年份列表数据访问异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据查询失败", e);
        } catch (Exception e) {
            logger.error("查询年份列表未知异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统内部错误", e);
        }
    }
    
    /**
     * 根据年份统计活动数量
     * @param year 年份
     * @return 活动数量
     */
    @Transactional(readOnly = true)
    public int countByYear(Integer year) {
        
        try {
            // 参数校验
            if (year == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, "年份不能为空");
            }
            
            return pastActivityMapper.countByYear(year);
            
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            logger.error("统计年份活动数量数据访问异常 - 年份: {}", year, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据查询失败", e);
        } catch (Exception e) {
            logger.error("统计年份活动数量未知异常 - 年份: {}", year, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统内部错误", e);
        }
    }
}