package com.redmoon2333.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.redmoon2333.entity.ActivationCode;
import com.redmoon2333.enums.ActivationStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 激活码 Mapper 接口
 * 提供激活码相关的数据访问方法
 */
@Mapper
public interface ActivationCodeMapper extends BaseMapper<ActivationCode> {
    
    /**
     * 根据激活码查找
     * @param code 激活码
     * @return 激活码实体
     */
    ActivationCode findByCode(@Param("code") String code);
    
    /**
     * 查找有效的激活码（未使用且未过期）
     * @param code 激活码
     * @param status 状态
     * @param now 当前时间
     * @return 激活码实体
     */
    ActivationCode findValidCode(@Param("code") String code, 
                                @Param("status") ActivationStatus status, 
                                @Param("now") LocalDateTime now);
    
    /**
     * 根据创建于查找激活码
     * @param creatorId 创建于 ID
     * @return 激活码列表
     */
    List<ActivationCode> findByCreatorId(@Param("creatorId") Integer creatorId);

    /**
     * 批量更新过期激活码状态
     * @param fromStatus 原状态（未使用）
     * @param toStatus 新状态（已过期）
     * @param now 当前时间，用于判断过期
     * @return 更新数量
     */
    int updateExpiredCodes(@Param("fromStatus") ActivationStatus fromStatus,
                           @Param("toStatus") ActivationStatus toStatus,
                           @Param("now") LocalDateTime now);
}