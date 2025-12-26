package com.redmoon2333.service;

import com.redmoon2333.dto.LoginRequest;
import com.redmoon2333.dto.RegisterRequest;
import com.redmoon2333.entity.ActivationCode;
import com.redmoon2333.entity.User;
import com.redmoon2333.enums.ActivationStatus;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
import com.redmoon2333.mapper.ActivationCodeMapper;
import com.redmoon2333.mapper.UserMapper;
import com.redmoon2333.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务类
 * 提供用户认证相关服务，包括登录、注册、生成激活码和获取用户信息
 */
@Service
@Transactional
public class AuthService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private ActivationCodeMapper activationCodeMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 用户登录
     * @param loginRequest 登录请求参数
     * @return 包含JWT令牌和用户信息的Map
     * @throws BusinessException 登录失败时抛出异常
     */
    public Map<String, Object> login(LoginRequest loginRequest) {
        // 查找用户
        User user = userMapper.findByUsername(loginRequest.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        // 验证密码
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        
        // 生成JWT令牌
        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRoleHistory());
        
        // 构造返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        result.put("tokenType", "Bearer");
        
        return result;
    }
    
    /**
     * 用户注册
     * @param registerRequest 注册请求参数
     * @return 注册的用户信息
     * @throws BusinessException 注册失败时抛出异常
     */
    public User register(RegisterRequest registerRequest) {
        // 验证密码确认
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        
        // 检查用户名是否已存在
        if (userMapper.countByUsername(registerRequest.getUsername()) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        
        // 验证激活码
        ActivationCode activationCode = activationCodeMapper.findByCode(registerRequest.getActivationCode());
        
        // 检查激活码是否存在
        if (activationCode == null) {
            throw new BusinessException(ErrorCode.INVALID_ACTIVATION_CODE);
        }
        
        // 检查激活码是否已使用
        if (activationCode.getStatus() == ActivationStatus.已使用) {
            throw new BusinessException(ErrorCode.INVALID_ACTIVATION_CODE, "激活码已被使用");
        }
        
        // 检查激活码是否过期（使用isAfter判断）
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(activationCode.getExpireTime())) {
            throw new BusinessException(ErrorCode.INVALID_ACTIVATION_CODE, "激活码已过期，过期时间: " + activationCode.getExpireTime() + "，当前时间: " + now);
        }
        
        // 创建新用户
        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setName(registerRequest.getName()); // 确保设置了姓名
        newUser.setRoleHistory(registerRequest.getRoleHistory());
        
        // 打印调试信息
        System.out.println("Registering user with name: " + registerRequest.getName());
        
        int insertResult = userMapper.insert(newUser);
        if (insertResult <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户创建失败");
        }
        
        // 更新激活码状态
        activationCode.setStatus(ActivationStatus.已使用);
        activationCode.setUserId(newUser.getUserId());
        activationCode.setUseTime(LocalDateTime.now());
        activationCodeMapper.update(activationCode);
        
        return newUser;
    }
    
    /**
     * 从令牌中获取用户信息
     * @param token JWT令牌
     * @return 用户信息
     * @throws BusinessException 令牌无效时抛出异常
     */
    public User getUserFromToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        
        String username = jwtUtil.getUsernameFromToken(token);
        return userMapper.findByUsername(username);
    }
    
    /**
     * 更新用户信息
     * @param user 用户信息
     * @return 更新后的用户信息
     * @throws BusinessException 更新失败时抛出异常
     */
    public User updateUserInfo(User user) {
        int result = userMapper.update(user);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户信息更新失败");
        }
        return userMapper.findById(user.getUserId());
    }
    
    /**
     * 生成激活码
     * @param token 管理员的JWT令牌
     * @param expireDays 激活码有效天数
     * @return 生成的激活码
     * @throws BusinessException 权限不足或令牌无效时抛出异常
     */
    public ActivationCode generateActivationCode(String token, int expireDays) {
        // 验证令牌
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        
        // 获取用户信息
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 检查用户是否具有管理员权限（通过角色历史检查是否包含"部长"关键词，包括副部长）
        String roleHistory = user.getRoleHistory();
        if (roleHistory == null || !roleHistory.contains("部长")) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_PERMISSIONS);
        }
        
        // 生成激活码
        String code = generateRandomCode();
        
        // 创建激活码实体
        ActivationCode activationCode = new ActivationCode();
        activationCode.setCode(code);
        activationCode.setCreatorId(user.getUserId());
        activationCode.setExpireTime(LocalDateTime.now().plusDays(expireDays));
        activationCode.setCreateTime(LocalDateTime.now());
        activationCode.setStatus(ActivationStatus.未使用);
        
        // 保存到数据库
        int result = activationCodeMapper.insert(activationCode);
        if (result <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "激活码生成失败");
        }
        
        // 重新查询以获取完整的激活码信息（包括创建时间等）
        return activationCodeMapper.findByCode(code);
    }
    
    /**
     * 生成随机激活码
     * @return 随机激活码
     */
    private String generateRandomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            int index = (int) (Math.random() * chars.length());
            code.append(chars.charAt(index));
        }
        return code.toString();
    }
}