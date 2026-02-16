package com.redmoon2333.mapper;

import com.redmoon2333.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
     * 根据姓名查找用户
     * @param name 姓名
     * @return 用户实体列表
     */
    List<User> findByName(@Param("name") String name);
    
    /**
     * 根据姓名模糊查找用户
     * @param name 姓名关键词
     * @return 用户实体列表
     */
    List<User> findByNameLike(@Param("name") String name);
    
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
    
    /**
     * 批量根据用户ID查找用户
     * @param userIds 用户ID列表
     * @return 用户实体列表
     */
    List<User> findByIds(@Param("userIds") List<Integer> userIds);
    
    /**
     * 获取所有用户
     * @return 所有用户列表
     */
    List<User> selectAll();
}