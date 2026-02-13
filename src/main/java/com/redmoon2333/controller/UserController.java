package com.redmoon2333.controller;

import com.redmoon2333.dto.ActivationCodeResponse;
import com.redmoon2333.dto.ActivationCodeListResponse;
import com.redmoon2333.dto.ActivationCodeStatsResponse;
import com.redmoon2333.dto.AlumniResponse;
import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.dto.PublicUserInfo;
import com.redmoon2333.entity.User;
import com.redmoon2333.service.UserService;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.util.RedisMemoryCleanupTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
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
    
    @Autowired(required = false)
    private RedisMemoryCleanupTask redisMemoryCleanupTask;
    
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
    
    /**
     * 获取该用户生成的所有激活码
     *
     * @param authHeader 授权令牌
     * @return 该用户生成的激活码列表及统计数据
     */
    @GetMapping("/activation-codes")
    public ApiResponse<ActivationCodeListResponse> getActivationCodes(
            @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("收到获取用户激活码的请求");
            String token = authHeader.replace("Bearer ", "");
            List<ActivationCodeResponse> codes = userService.getActivationCodesByUser(token);
            ActivationCodeStatsResponse stats = userService.getActivationCodeStats(token);
            logger.info("成功获取该用户的 {} 个激活码", codes.size());
            logger.info("统计数据: 总数={}, 已使用={}, 未使用={}, 已过期={}", 
                stats.getTotalCount(), stats.getUsedCount(), stats.getUnusedCount(), stats.getExpiredCount());
            ActivationCodeListResponse response = new ActivationCodeListResponse(codes, stats);
            logger.debug("响应数据: codes={}, stats={}", response.getCodes().size(), response.getStats());
            return ApiResponse.success("查询成功", response);
        } catch (BusinessException e) {
            logger.warn("获取用户激活码失败: {}", e.getErrorCode().getMessage(), e);
            return ApiResponse.error(e.getErrorCode().getMessage(), e.getErrorCode().getCode());
        } catch (Exception e) {
            logger.error("获取用户激活码时发生异常", e);
            return ApiResponse.error("系统内部错误", 500);
        }
    }
    
    /**
     * 删除激活码
     * 
     * @param authHeader 授权令牌
     * @param codeId 激活码ID
     * @return 删除结果
     */
    @PutMapping("/activation-codes/{codeId}/delete")
    public ApiResponse<String> deleteActivationCode(
            @RequestHeader("Authorization") String authHeader,
            @org.springframework.web.bind.annotation.PathVariable String codeId) {
        try {
            logger.info("收到删除激活码的请求，激活码ID: {}", codeId);
            
            // 验证codeId是否为有效整数
            Integer codeIdInt;
            try {
                codeIdInt = Integer.parseInt(codeId);
            } catch (NumberFormatException e) {
                logger.error("无效的激活码ID: {}", codeId);
                return ApiResponse.error("无效的激活码ID", 400);
            }
            
            String token = authHeader.replace("Bearer ", "");
            userService.deleteActivationCode(token, codeIdInt);
            logger.info("成功删除激活码，ID: {}", codeIdInt);
            return ApiResponse.success("激活码删除成功", null);
        } catch (BusinessException e) {
            logger.warn("删除激活码失败: {}", e.getErrorCode().getMessage(), e);
            return ApiResponse.error(e.getErrorCode().getMessage(), e.getErrorCode().getCode());
        } catch (Exception e) {
            logger.error("删除激活码时发生异常", e);
            return ApiResponse.error("系统内部错误", 500);
        }
    }
    
    /**
     * 手动清理Redis内存中的过期对话
     * 仅部长可访问
     * 
     * @return 清理结果
     */
    @PostMapping("/cleanup-memory")
    public ApiResponse<Map<String, Object>> cleanupRedisMemory() {
        try {
            logger.info("收到手动清理Redis内存的请求");
            
            if (redisMemoryCleanupTask == null) {
                logger.warn("Redis清理任务未初始化");
                return ApiResponse.error("清理功能不可用", 503);
            }
            
            int cleanedCount = redisMemoryCleanupTask.manualCleanup();
            
            Map<String, Object> result = new HashMap<>();
            result.put("cleanedCount", cleanedCount);
            result.put("message", "成功清理 " + cleanedCount + " 个过期会话");
            
            logger.info("手动清理完成，清理了 {} 个会话", cleanedCount);
            return ApiResponse.success("清理成功", result);
        } catch (Exception e) {
            logger.error("手动清理Redis内存时发生异常", e);
            return ApiResponse.error("清理失败: " + e.getMessage(), 500);
        }
    }
}