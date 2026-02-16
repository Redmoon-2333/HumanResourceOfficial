package com.redmoon2333.service;

import com.redmoon2333.dto.ActivationCodeResponse;
import com.redmoon2333.dto.ActivationCodeStatsResponse;
import com.redmoon2333.dto.AlumniMember;
import com.redmoon2333.dto.AlumniResponse;
import com.redmoon2333.dto.PublicUserInfo;
import com.redmoon2333.entity.User;
import com.redmoon2333.mapper.UserMapper;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户服务类
 * 提供用户相关服务
 */
@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private com.redmoon2333.mapper.ActivationCodeMapper activationCodeMapper;
    
    /**
     * 获取往届部员信息
     * 从所有用户的roleHistory中提取出任职经历，然后把同一年的人放到一起
     * 同一人有多重身份时，只算作1位成员
     * 
     * @return 按年份分组的部员信息列表
     */
    public List<AlumniResponse> getAlumniMembers() {
        try {
            logger.info("开始获取往届部员信息");
            
            List<User> allUsers = getAllUsers();
            logger.debug("共获取到 {} 个用户", allUsers.size());
            
            if (logger.isDebugEnabled()) {
                logger.debug("获取到的所有用户信息:");
                for (User user : allUsers) {
                    logger.debug("用户ID: {}, 用户名: {}, 角色历史: {}", 
                        user.getUserId(), user.getUsername(), user.getRoleHistory());
                }
            }
            
            Map<Integer, List<AlumniMember>> alumniMap = new HashMap<>();
            Set<String> allUniqueNames = new HashSet<>();
            
            Pattern rolePattern = Pattern.compile("(\\d{4})级(.+)");
            
            int memberCount = 0;
            for (User user : allUsers) {
                String roleHistory = user.getRoleHistory();
                logger.debug("处理用户 {} 的角色历史: {}", user.getUsername(), roleHistory);
                
                if (roleHistory == null || roleHistory.trim().isEmpty()) {
                    logger.debug("用户 {} 没有角色历史信息", user.getUsername());
                    continue;
                }
                
                List<String> roleEntries = parseRoleHistory(roleHistory);
                logger.debug("用户 {} 解析出 {} 个角色条目", user.getUsername(), roleEntries.size());
                
                for (String roleEntry : roleEntries) {
                    roleEntry = roleEntry.trim();
                    if (roleEntry.isEmpty()) continue;
                    
                    logger.debug("处理角色条目: {}", roleEntry);
                    
                    Matcher matcher = rolePattern.matcher(roleEntry);
                    
                    if (matcher.matches()) {
                        try {
                            Integer year = Integer.valueOf(matcher.group(1));
                            String roleName = matcher.group(2).trim();
                            
                            roleName = roleName.replace("[", "").replace("]", "").replace("\"", "").replace("'", "").trim();
                            
                            logger.debug("解析出年份: {}, 角色: {}", year, roleName);
                            
                            AlumniMember member = new AlumniMember(user.getName(), roleName);
                            
                            alumniMap.computeIfAbsent(year, k -> new ArrayList<>()).add(member);
                            allUniqueNames.add(user.getName());
                            memberCount++;
                            
                            logger.debug("添加部员信息: 用户={}, 年份={}, 角色={}", user.getName(), year, roleName);
                        } catch (NumberFormatException e) {
                            logger.warn("解析年份失败，角色信息: {}", roleEntry, e);
                        } catch (Exception e) {
                            logger.warn("处理角色信息时发生异常，角色信息: {}", roleEntry, e);
                        }
                    } else {
                        logger.debug("角色信息格式不匹配: {}", roleEntry);
                    }
                }
            }
            
            logger.info("共处理 {} 个身份记录，涉及 {} 个年份，唯一成员 {} 人", memberCount, alumniMap.size(), allUniqueNames.size());
            
            if (logger.isDebugEnabled()) {
                logger.debug("年份分组信息:");
                for (Map.Entry<Integer, List<AlumniMember>> entry : alumniMap.entrySet()) {
                    logger.debug("年份: {}, 部员数量: {}", entry.getKey(), entry.getValue().size());
                    for (AlumniMember member : entry.getValue()) {
                        logger.debug("  部员: {}, 角色: {}", member.getName(), member.getRole());
                    }
                }
            }
            
            List<AlumniResponse> result = new ArrayList<>();
            List<Integer> sortedYears = new ArrayList<>(alumniMap.keySet());
            Collections.sort(sortedYears, Collections.reverseOrder());
            
            for (Integer year : sortedYears) {
                List<AlumniMember> yearMembers = alumniMap.get(year);
                Set<String> yearUniqueNames = new HashSet<>();
                for (AlumniMember m : yearMembers) {
                    yearUniqueNames.add(m.getName());
                }
                AlumniResponse response = new AlumniResponse(year, yearMembers, yearUniqueNames.size());
                result.add(response);
            }
            
            logger.info("成功生成往届部员信息，共 {} 个年份", result.size());
            
            if (logger.isDebugEnabled()) {
                logger.debug("最终结果:");
                for (AlumniResponse response : result) {
                    logger.debug("年份: {}, 身份记录数: {}, 唯一成员数: {}", 
                        response.getYear(), response.getMembers().size(), response.getUniqueMemberCount());
                    for (AlumniMember member : response.getMembers()) {
                        logger.debug("  部员: {}, 角色: {}", member.getName(), member.getRole());
                    }
                }
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("获取往届部员信息时发生异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取往届部员信息失败", e);
        }
    }
    
    /**
     * 解析角色历史字符串
     * 支持格式：
     * 1. JSON数组: ["2024级部员&2025级部长"]
     * 2. JSON数组: ["2024级部员", "2025级部长"]
     * 3. 普通字符串: 2024级部员&2025级部长
     * 4. 普通字符串: 2024级部员,2025级部长
     * 
     * @param roleHistory 角色历史字符串
     * @return 解析后的角色条目列表
     */
    private List<String> parseRoleHistory(String roleHistory) {
        List<String> result = new ArrayList<>();
        if (roleHistory == null || roleHistory.trim().isEmpty()) {
            return result;
        }
        
        String cleaned = roleHistory.trim();
        
        // 尝试解析JSON数组格式
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            try {
                // 移除外层的方括号
                cleaned = cleaned.substring(1, cleaned.length() - 1);
                
                // 分割JSON数组元素
                // 处理格式: "2024级部员&2025级部长" 或 "2024级部员","2025级部长"
                List<String> jsonElements = new ArrayList<>();
                StringBuilder current = new StringBuilder();
                boolean inQuotes = false;
                
                for (int i = 0; i < cleaned.length(); i++) {
                    char c = cleaned.charAt(i);
                    if (c == '"' && (i == 0 || cleaned.charAt(i - 1) != '\\')) {
                        inQuotes = !inQuotes;
                    } else if (c == ',' && !inQuotes) {
                        jsonElements.add(current.toString().trim());
                        current = new StringBuilder();
                    } else {
                        current.append(c);
                    }
                }
                if (current.length() > 0) {
                    jsonElements.add(current.toString().trim());
                }
                
                // 处理每个JSON元素，移除引号并按 & 分割
                for (String element : jsonElements) {
                    element = element.replace("\"", "").replace("'", "").trim();
                    if (element.contains("&")) {
                        // 格式: 2024级部员&2025级部长
                        String[] parts = element.split("&");
                        for (String part : parts) {
                            part = part.trim();
                            if (!part.isEmpty()) {
                                result.add(part);
                            }
                        }
                    } else if (!element.isEmpty()) {
                        result.add(element);
                    }
                }
                
                return result;
            } catch (Exception e) {
                logger.warn("解析JSON数组格式失败，尝试普通格式: {}", roleHistory, e);
            }
        }
        
        // 普通格式处理
        cleaned = cleaned.replace("[", "").replace("]", "").replace("\"", "").replace("'", "").trim();
        
        // 按 & 或 , 分割
        String[] parts = cleaned.split("[&,]");
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        
        return result;
    }
    
    /**
     * 根据姓名查找用户
     * 
     * @param name 姓名
     * @return 用户列表
     */
    public List<PublicUserInfo> searchUsersByName(String name) {
        try {
            logger.debug("开始根据姓名查找用户: {}", name);
            List<User> users = userMapper.findByName(name);
            // 转换为不包含敏感信息的公共用户信息
            List<PublicUserInfo> publicUsers = users.stream()
                .map(user -> new PublicUserInfo(user.getName(), user.getRoleHistory()))
                .collect(Collectors.toList());
            logger.debug("成功查找到 {} 个用户", publicUsers.size());
            return publicUsers;
        } catch (Exception e) {
            logger.error("根据姓名查找用户时发生异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查找用户失败", e);
        }
    }

    /**
     * 根据姓名模糊查找用户
     * 
     * @param name 姓名关键词
     * @return 用户列表
     */
    public List<PublicUserInfo> searchUsersByNameLike(String name) {
        try {
            logger.debug("开始根据姓名模糊查找用户: {}", name);
            List<User> users = userMapper.findByNameLike(name);
            // 转换为不包含敏感信息的公共用户信息
            List<PublicUserInfo> publicUsers = users.stream()
                .map(user -> new PublicUserInfo(user.getName(), user.getRoleHistory()))
                .collect(Collectors.toList());
            logger.debug("成功查找到 {} 个用户", publicUsers.size());
            return publicUsers;
        } catch (Exception e) {
            logger.error("根据姓名模糊查找用户时发生异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "模糊查找用户失败", e);
        }
    }
    
    /**
     * 获取所有用户
     * 
     * @return 所有用户列表
     */
    private List<User> getAllUsers() {
        try {
            logger.debug("开始获取所有用户信息");
            List<User> users = userMapper.selectAll();
            logger.debug("成功获取所有用户信息，共 {} 个用户", users.size());
            return users;
        } catch (Exception e) {
            logger.error("获取所有用户信息时发生异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取用户信息失败", e);
        }
    }
    
    /**
     * 调试方法：获取用户总数
     * 
     * @return 用户总数
     */
    public int getUserCount() {
        try {
            List<User> users = userMapper.selectAll();
            return users.size();
        } catch (Exception e) {
            logger.error("获取用户总数时发生异常", e);
            return 0;
        }
    }
    
    /**
     * 获取用户生成的激活码列表
     *
     * @param token JWT令牌
     * @return 激活码响应DTO列表
     */
    public List<ActivationCodeResponse> getActivationCodesByUser(String token) {
        try {
            logger.debug("开始获取用户激活码");

            // 获取用户
            User user = authService.getUserFromToken(token);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }

            // 查询用户生成的激活码
            List<com.redmoon2333.entity.ActivationCode> codes = activationCodeMapper.findByCreatorId(user.getUserId());
            logger.info("成功获取用户 {} 的 {} 个激活码", user.getUsername(), codes.size());

            // 批量查询使用者信息，避免 N+1 查询
            Set<Integer> userIds = codes.stream()
                .filter(code -> code.getUserId() != null)
                .map(com.redmoon2333.entity.ActivationCode::getUserId)
                .collect(Collectors.toSet());
            
            final Map<Integer, String> userNameMap;
            if (!userIds.isEmpty()) {
                List<User> users = userMapper.findByIds(new ArrayList<>(userIds));
                userNameMap = users.stream()
                    .collect(Collectors.toMap(User::getUserId, User::getUsername));
            } else {
                userNameMap = new HashMap<>();
            }

            // 转换为DTO
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return codes.stream().map(code -> {
                ActivationCodeResponse dto = new ActivationCodeResponse();
                dto.setId(code.getCodeId());
                dto.setCode(code.getCode());
                dto.setCreatedBy(code.getCreatorId());
                dto.setCreatorName(user.getUsername());
                dto.setCreateTime(code.getCreateTime() != null ? code.getCreateTime().format(formatter) : null);
                dto.setExpireTime(code.getExpireTime() != null ? code.getExpireTime().format(formatter) : null);
                dto.setUsed(code.getStatus() == com.redmoon2333.enums.ActivationStatus.已使用);
                dto.setUsedBy(code.getUserId());
                dto.setUsedByName(userNameMap.get(code.getUserId()));
                dto.setUsedTime(code.getUseTime() != null ? code.getUseTime().format(formatter) : null);
                return dto;
            }).collect(Collectors.toList());
        } catch (BusinessException e) {
            logger.warn("获取用户激活码失败: {}", e.getErrorCode().getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("获取用户激活码时发生异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取激活码失败", e);
        }
    }

    /**
     * 获取用户生成的激活码统计数据
     *
     * @param token JWT令牌
     * @return 激活码统计数据
     */
    public ActivationCodeStatsResponse getActivationCodeStats(String token) {
        try {
            logger.debug("开始获取用户激活码统计数据");

            User user = authService.getUserFromToken(token);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }

            List<com.redmoon2333.entity.ActivationCode> codes = activationCodeMapper.findByCreatorId(user.getUserId());

            int totalCount = codes.size();
            int usedCount = 0;
            int unusedCount = 0;
            int expiredCount = 0;

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            for (com.redmoon2333.entity.ActivationCode code : codes) {
                if (code.getStatus() == com.redmoon2333.enums.ActivationStatus.已使用) {
                    usedCount++;
                } else if (code.getExpireTime() != null && code.getExpireTime().isBefore(now)) {
                    expiredCount++;
                } else {
                    unusedCount++;
                }
            }

            logger.info("用户 {} 的激活码统计: 总数={}, 已使用={}, 未使用={}, 已过期={}",
                    user.getUsername(), totalCount, usedCount, unusedCount, expiredCount);

            return new ActivationCodeStatsResponse(totalCount, unusedCount, usedCount, expiredCount);
        } catch (BusinessException e) {
            logger.warn("获取激活码统计数据失败: {}", e.getErrorCode().getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("获取激活码统计数据时发生异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取激活码统计数据失败", e);
        }
    }
    
    /**
     * 删除激活码
     * 
     * @param token JWT令牦
     * @param codeId 激活码ID
     */
    public void deleteActivationCode(String token, Integer codeId) {
        try {
            logger.debug("开始删除激活码，ID: {}", codeId);
            
            // 获取用户
            User user = authService.getUserFromToken(token);
            if (user == null) {
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }
            
            // 检验激活码是否存在且是由该用户生成
            com.redmoon2333.entity.ActivationCode code = activationCodeMapper.findById(codeId);
            if (code == null) {
                throw new BusinessException(ErrorCode.INVALID_ACTIVATION_CODE, "激活码不存在");
            }
            if (!code.getCreatorId().equals(user.getUserId())) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_PERMISSIONS, "你没有权限删除该激活码");
            }
            
            // 删除激活码
            int result = activationCodeMapper.deleteById(codeId);
            if (result <= 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除激活码失败");
            }
            
            logger.info("成功删除激活码，ID: {}", codeId);
        } catch (BusinessException e) {
            logger.warn("删除激活码失败: {}", e.getErrorCode().getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("删除激活码时发生异常", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除激活码失败", e);
        }
    }
    
    /**
     * 调试方法：获取所有用户详细信息
     * 
     * @return 所有用户列表
     */
    public List<User> getAllUsersDebug() {
        try {
            logger.debug("开始获取所有用户信息（调试模式）");
            List<User> users = userMapper.selectAll();
            logger.debug("成功获取所有用户信息（调试模式），共 {} 个用户", users.size());
            // 添加调试输出
            for (User user : users) {
                logger.debug("用户详情 - ID: {}, 用户名: {}, 姓名: {}, 角色历史: {}", 
                    user.getUserId(), user.getUsername(), user.getName(), user.getRoleHistory());
            }
            return users;
        } catch (Exception e) {
            logger.error("获取所有用户信息时发生异常（调试模式）", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取用户信息失败", e);
        }
    }
}