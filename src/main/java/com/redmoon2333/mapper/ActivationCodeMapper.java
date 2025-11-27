package com.redmoon2333.mapper;

import com.redmoon2333.entity.ActivationCode;
import com.redmoon2333.enums.ActivationStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 激活码Mapper接口
 * 提供激活码相关的数据访问方法
 */
@Mapper
public interface ActivationCodeMapper {
    
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
     * 插入新激活码
     * @param activationCode 激活码实体
     * @return 影响行数
     */
    int insert(ActivationCode activationCode);
    
    /**
     * 更新激活码状态
     * @param activationCode 激活码实体
     * @return 影响行数
     */
    int update(ActivationCode activationCode);
    
    /**
     * 根据创建于查找激活码
     * @param creatorId 创建于ID
     * @return 激活码列表
     */
    List<ActivationCode> findByCreatorId(@Param("creatorId") Integer creatorId);
    
    /**
     * 根据ID查找激活码
     * @param codeId 激活码ID
     * @return 激活码实体
     */
    ActivationCode findById(@Param("codeId") Integer codeId);
    
    /**
     * 根据ID删除激活码
     * @param codeId 激活码ID
     * @return 影响行数
     */
    int deleteById(@Param("codeId") Integer codeId);
}