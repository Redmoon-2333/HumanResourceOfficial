package com.redmoon2333.controller;

import com.redmoon2333.dto.AlumniResponse;
import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.dto.PublicUserInfo;
import com.redmoon2333.entity.User;
import com.redmoon2333.service.UserService;
import com.redmoon2333.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 用户控制器
 * 提供用户相关接口
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService userService;
    
    /**
     * 调试接口：获取所有用户信息
     * 用于排查数据库数据问题
     * 
     * @return 所有用户的详细信息
     */
    @GetMapping("/debug/all")
    public ApiResponse<?> getAllUsersDebug() {
        try {
            logger.info("收到调试请求：获取所有用户信息");
            
            // 获取用户总数
            int userCount = userService.getUserCount();
            logger.info("数据库中用户总数: {}", userCount);
            
            // 获取所有用户详细信息
            List<User> allUsers = userService.getAllUsersDebug();
            logger.info("实际查询到的用户数: {}", allUsers.size());
            
            // 输出每个用户的详细信息
            for (com.redmoon2333.entity.User user : allUsers) {
                logger.info("用户详情 - ID: {}, 用户名: {}, 姓名: {}, 角色历史: '{}'", 
                    user.getUserId(), user.getUsername(), user.getName(), user.getRoleHistory());
            }
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("totalCount", userCount);
            debugInfo.put("actualUsers", allUsers);
            debugInfo.put("usersWithRoleHistory", 
                allUsers.stream().filter(u -> u.getRoleHistory() != null && !u.getRoleHistory().trim().isEmpty()).count());
            
            return ApiResponse.success("调试信息获取成功", debugInfo);
        } catch (Exception e) {
            logger.error("获取调试信息时发生异常", e);
            return ApiResponse.error("获取调试信息失败: " + e.getMessage(), 500);
        }
    }


    /**
     * 获取往届部员信息
     * 按年份分组展示所有部员、部长和副部长
     * 
     * @return 按年份分组的部员信息列表
     */
    @GetMapping("/alumni")
    public ApiResponse<List<AlumniResponse>> getAlumniMembers() {
        try {
            logger.info("收到获取往届部员信息的请求");
            List<AlumniResponse> alumniMembers = userService.getAlumniMembers();
            logger.info("成功获取往届部员信息，共 {} 个年份", alumniMembers.size());
            return ApiResponse.success("查询成功", alumniMembers);
        } catch (BusinessException e) {
            logger.warn("获取往届部员信息失败: {}", e.getErrorCode().getMessage(), e);
            return ApiResponse.error(e.getErrorCode().getMessage(), e.getErrorCode().getCode());
        } catch (Exception e) {
            logger.error("获取往届部员信息时发生未预期的异常", e);
            return ApiResponse.error("系统内部错误", 500);
        }
    }
    
    /**
     * 根据姓名查找用户
     * 
     * @param name 姓名
     * @return 查找到的用户列表
     */
    @GetMapping("/search/name")
    public ApiResponse<List<PublicUserInfo>> searchUsersByName(@RequestParam String name) {
        try {
            logger.info("收到根据姓名查找用户的请求，姓名: {}", name);
            List<PublicUserInfo> users = userService.searchUsersByName(name);
            logger.info("成功查找到 {} 个用户", users.size());
            return ApiResponse.success("查找成功", users);
        } catch (BusinessException e) {
            logger.warn("根据姓名查找用户失败: {}", e.getErrorCode().getMessage(), e);
            return ApiResponse.error(e.getErrorCode().getMessage(), e.getErrorCode().getCode());
        } catch (Exception e) {
            logger.error("根据姓名查找用户时发生未预期的异常", e);
            return ApiResponse.error("系统内部错误", 500);
        }
    }
    
    /**
     * 根据姓名模糊查找用户
     * 
     * @param name 姓名关键词
     * @return 查找到的用户列表
     */
    @GetMapping("/search/name/like")
    public ApiResponse<List<PublicUserInfo>> searchUsersByNameLike(@RequestParam String name) {
        try {
            logger.info("收到根据姓名模糊查找用户的请求，关键词: {}", name);
            List<PublicUserInfo> users = userService.searchUsersByNameLike(name);
            logger.info("成功查找到 {} 个用户", users.size());
            return ApiResponse.success("查找成功", users);
        } catch (BusinessException e) {
            logger.warn("根据姓名模糊查找用户失败: {}", e.getErrorCode().getMessage(), e);
            return ApiResponse.error(e.getErrorCode().getMessage(), e.getErrorCode().getCode());
        } catch (Exception e) {
            logger.error("根据姓名模糊查找用户时发生未预期的异常", e);
            return ApiResponse.error("系统内部错误", 500);
        }
    }
}