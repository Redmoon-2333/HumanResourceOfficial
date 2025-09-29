package com.redmoon2333.mapper;

import com.redmoon2333.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper接口
 * 提供用户相关的数据访问方法
 */
@Mapper
public interface UserMapper {
    
    /**
     * 根据用户ID查找用户
     * @param userId 用户ID
     * @return 用户实体
     */
    User findById(@Param("userId") Integer userId);
    
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 用户实体
     */
    User findByUsername(@Param("username") String username);
    
    /**
     * 检查用户名是否存在
     * @param username 用户名
     * @return 存在数量
     */
    int countByUsername(@Param("username") String username);
    
    /**
     * 插入新用户
     * @param user 用户实体
     * @return 影响行数
     */
    int insert(User user);
    
    /**
     * 更新用户信息
     * @param user 用户实体
     * @return 影响行数
     */
    int update(User user);
    
    /**
     * 删除用户
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteById(@Param("userId") Integer userId);
}