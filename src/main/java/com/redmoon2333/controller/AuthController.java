package com.redmoon2333.controller;

import com.redmoon2333.dto.ApiResponse;
import com.redmoon2333.dto.LoginRequest;
import com.redmoon2333.dto.RegisterRequest;
import com.redmoon2333.entity.ActivationCode;
import com.redmoon2333.entity.User;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.service.AuthService;
import com.redmoon2333.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 提供用户认证相关接口，包括登录、注册、生成激活码和获取当前用户信息
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 用户登录接口
     * @param loginRequest 登录请求参数，包含用户名和密码
     * @param bindingResult 参数验证结果
     * @return 登录结果，包含JWT令牌和用户信息
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                 BindingResult bindingResult) {
        try {
            // 参数验证
            if (bindingResult.hasErrors()) {
                return ApiResponse.error(bindingResult.getAllErrors().get(0).getDefaultMessage());
            }
            
            // 执行登录，获取包含JWT令牌的结果
            Map<String, Object> loginResult = authService.login(loginRequest);
            return ApiResponse.success("登录成功", loginResult);
            
        } catch (BusinessException e) {
            System.err.println("Business exception: " + e.getMessage());
            return ApiResponse.error(e.getErrorCode().getMessage(), e.getErrorCode().getCode());
        } catch (Exception e) {
            System.err.println("System exception: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("系统内部错误", 500);
        }
    }
    
    /**
     * 用户注册接口
     * @param registerRequest 注册请求参数，包含用户名、密码、确认密码、激活码和角色历史
     * @param bindingResult 参数验证结果
     * @return 注册结果
     */
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest registerRequest,
                                                    BindingResult bindingResult) {
        try {
            // 参数验证
            if (bindingResult.hasErrors()) {
                return ApiResponse.error(bindingResult.getAllErrors().get(0).getDefaultMessage());
            }
            
            // 执行注册
            User user = authService.register(registerRequest);
            
            // 构造返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getUserId());
            data.put("username", user.getUsername());
            data.put("roleHistory", user.getRoleHistory());
            data.put("name", user.getName());
            data.put("message", "注册成功，请登录");
            
            return ApiResponse.success("注册成功", data);
            
        } catch (BusinessException e) {
            return ApiResponse.error(e.getErrorCode().getMessage(), e.getErrorCode().getCode());
        } catch (Exception e) {
            return ApiResponse.error("系统内部错误", 500);
        }
    }
    
    /**
     * 获取当前用户信息接口
     * @param authHeader Authorization头，包含JWT令牌
     * @return 当前用户信息
     */
    @GetMapping("/current-user")
    public ApiResponse<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (!authHeader.startsWith("Bearer ")) {
                return ApiResponse.error("请提供有效的Authorization头", 401);
            }
            
            String token = authHeader.substring(7);
            User user = authService.getUserFromToken(token);
            
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getUserId());
            data.put("username", user.getUsername());
            data.put("name", user.getName());
            data.put("roleHistory", user.getRoleHistory());
            
            return ApiResponse.success(data);
            
        } catch (BusinessException e) {
            return ApiResponse.error(e.getErrorCode().getMessage(), e.getErrorCode().getCode());
        } catch (Exception e) {
            return ApiResponse.error("系统内部错误", 500);
        }
    }
    
    /**
     * 更新用户信息接口
     * @param authHeader Authorization头，包含JWT令牌
     * @param updateRequest 更新请求参数
     * @return 更新后的用户信息
     */
    @PutMapping("/update-profile")
    public ApiResponse<Map<String, Object>> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> updateRequest) {
        try {
            if (!authHeader.startsWith("Bearer ")) {
                return ApiResponse.error("请提供有效的Authorization头", 401);
            }
            
            String token = authHeader.substring(7);
            User currentUser = authService.getUserFromToken(token);
            
            // 更新姓名
            if (updateRequest.containsKey("name")) {
                currentUser.setName(updateRequest.get("name"));
            }
            
            // 更新往届身份
            if (updateRequest.containsKey("roleHistory")) {
                currentUser.setRoleHistory(updateRequest.get("roleHistory"));
            }
            
            // 调用service更新数据库
            User updatedUser = authService.updateUserInfo(currentUser);
            
            Map<String, Object> data = new HashMap<>();
            data.put("userId", updatedUser.getUserId());
            data.put("username", updatedUser.getUsername());
            data.put("name", updatedUser.getName());
            data.put("roleHistory", updatedUser.getRoleHistory());
            
            return ApiResponse.success("修改成功", data);
            
        } catch (BusinessException e) {
            return ApiResponse.error(e.getErrorCode().getMessage(), e.getErrorCode().getCode());
        } catch (Exception e) {
            return ApiResponse.error("系统内部错误", 500);
        }
    }
    
    /**
     * 管理员生成激活码接口
     * 只有具有"部长"角色的用户才能生成激活码
     * @param authHeader Authorization头，包含JWT令牌
     * @param expireDays 激活码有效天数，默认30天
     * @return 生成的激活码信息
     */
    @PostMapping("/generate-code")
    public ApiResponse<Map<String, Object>> generateActivationCode(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "30") int expireDays) {
        try {
            if (!authHeader.startsWith("Bearer ")) {
                return ApiResponse.error("请提供有效的Authorization头", 401);
            }
            
            String token = authHeader.substring(7);
            ActivationCode activationCode = authService.generateActivationCode(token, expireDays);
            
            Map<String, Object> data = new HashMap<>();
            data.put("code", activationCode.getCode());
            data.put("expireTime", activationCode.getExpireTime());
            data.put("message", "激活码生成成功");
            
            return ApiResponse.success("激活码生成成功", data);
            
        } catch (BusinessException e) {
            return ApiResponse.error(e.getErrorCode().getMessage(), e.getErrorCode().getCode());
        } catch (Exception e) {
            return ApiResponse.error("系统内部错误", 500);
        }
    }
    
    /**
     * 用户退出登录接口
     * 将JWT令牌加入黑名单，实现安全退出
     * @param authHeader Authorization头，包含JWT令牌
     * @return 退出结果
     */
    @PostMapping("/logout")
    public ApiResponse<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (!authHeader.startsWith("Bearer ")) {
                return ApiResponse.error("请提供有效的Authorization头", 401);
            }
            
            String token = authHeader.substring(7);
            
            // 验证令牌有效性
            if (!jwtUtil.validateToken(token)) {
                return ApiResponse.error("无效的令牌", 401);
            }
            
            // 将令牌加入黑名单
            jwtUtil.logout(token);
            
            Map<String, Object> data = new HashMap<>();
            data.put("message", "退出登录成功");
            
            return ApiResponse.success("退出登录成功", data);
            
        } catch (BusinessException e) {
            return ApiResponse.error(e.getErrorCode().getMessage(), e.getErrorCode().getCode());
        } catch (Exception e) {
            return ApiResponse.error("系统内部错误", 500);
        }
    }
    
    /**
     * 管理员强制用户下线接口
     * 只有具有"部长"角色的用户才能强制其他用户下线
     * @param authHeader Authorization头，包含JWT令牌
     * @param targetUserId 目标用户ID
     * @return 操作结果
     */
    @PostMapping("/revoke-user/{targetUserId}")
    public ApiResponse<Map<String, Object>> revokeUserTokens(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Integer targetUserId) {
        try {
            if (!authHeader.startsWith("Bearer ")) {
                return ApiResponse.error("请提供有效的Authorization头", 401);
            }
            
            String token = authHeader.substring(7);
            
            // 验证令牌有效性
            if (!jwtUtil.validateToken(token)) {
                return ApiResponse.error("无效的令牌", 401);
            }
            
            // 检查是否具有部长权限
            if (!jwtUtil.hasRole(token, "部长")) {
                return ApiResponse.error("权限不足，只有部长才能撤销用户令牌", 403);
            }
            
            // 强制目标用户下线
            jwtUtil.revokeUserTokens(targetUserId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("targetUserId", targetUserId);
            data.put("message", "用户令牌已被撤销，用户已强制下线");
            
            return ApiResponse.success("操作成功", data);
            
        } catch (BusinessException e) {
            return ApiResponse.error(e.getErrorCode().getMessage(), e.getErrorCode().getCode());
        } catch (Exception e) {
            return ApiResponse.error("系统内部错误", 500);
        }
    }
}