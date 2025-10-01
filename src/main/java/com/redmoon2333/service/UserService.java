package com.redmoon2333.service;

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
    
    /**
     * 获取往届部员信息
     * 从所有用户的roleHistory中提取出任职经历，然后把同一年的人放到一起
     * 
     * @return 按年份分组的部员信息列表
     */
    public List<AlumniResponse> getAlumniMembers() {
        try {
            logger.info("开始获取往届部员信息");
            
            // 获取所有用户
            List<User> allUsers = getAllUsers();
            logger.debug("共获取到 {} 个用户", allUsers.size());
            
            // 记录所有用户信息用于调试
            if (logger.isDebugEnabled()) {
                logger.debug("获取到的所有用户信息:");
                for (User user : allUsers) {
                    logger.debug("用户ID: {}, 用户名: {}, 角色历史: {}", 
                        user.getUserId(), user.getUsername(), user.getRoleHistory());
                }
            }
            
            // 用于存储按年份分组的部员信息
            Map<Integer, List<AlumniMember>> alumniMap = new HashMap<>();
            
            // 正则表达式匹配"年份+角色"的格式，例如"2024级部长"
            Pattern rolePattern = Pattern.compile("(\\d{4})级(.+)");
            
            // 遍历所有用户，解析他们的角色历史
            int memberCount = 0;
            for (User user : allUsers) {
                String roleHistory = user.getRoleHistory();
                logger.debug("处理用户 {} 的角色历史: {}", user.getUsername(), roleHistory);
                
                if (roleHistory == null || roleHistory.trim().isEmpty()) {
                    logger.debug("用户 {} 没有角色历史信息", user.getUsername());
                    continue;
                }
                
                // 分割角色历史，可能包含多个角色
                String[] roles = roleHistory.split("&");
                logger.debug("用户 {} 有 {} 个角色", user.getUsername(), roles.length);
                
                // 处理每个角色
                for (String role : roles) {
                    role = role.trim();
                    logger.debug("处理角色: {}", role);
                    Matcher matcher = rolePattern.matcher(role);
                    
                    if (matcher.matches()) {
                        try {
                            // 提取年份和角色
                            Integer year = Integer.valueOf(matcher.group(1));
                            String roleName = matcher.group(2);
                            
                            logger.debug("解析出年份: {}, 角色: {}", year, roleName);
                            
                            // 创建部员信息
                            AlumniMember member = new AlumniMember(user.getName(), roleName);
                            
                            // 将部员信息添加到对应年份的列表中
                            alumniMap.computeIfAbsent(year, k -> new ArrayList<>()).add(member);
                            memberCount++;
                            
                            logger.debug("添加部员信息: 用户={}, 年份={}, 角色={}", user.getName(), year, roleName);
                        } catch (NumberFormatException e) {
                            logger.warn("解析年份失败，角色信息: {}", role, e);
                        } catch (Exception e) {
                            logger.warn("处理角色信息时发生异常，角色信息: {}", role, e);
                        }
                    } else {
                        logger.debug("角色信息格式不匹配: {}", role);
                    }
                }
            }
            
            logger.info("共处理 {} 个部员信息，涉及 {} 个年份", memberCount, alumniMap.size());
            
            // 记录年份分组信息用于调试
            if (logger.isDebugEnabled()) {
                logger.debug("年份分组信息:");
                for (Map.Entry<Integer, List<AlumniMember>> entry : alumniMap.entrySet()) {
                    logger.debug("年份: {}, 部员数量: {}", entry.getKey(), entry.getValue().size());
                    for (AlumniMember member : entry.getValue()) {
                        logger.debug("  部员: {}, 角色: {}", member.getName(), member.getRole());
                    }
                }
            }
            
            // 将Map转换为List，并按年份降序排序
            List<AlumniResponse> result = new ArrayList<>();
            List<Integer> sortedYears = new ArrayList<>(alumniMap.keySet());
            Collections.sort(sortedYears, Collections.reverseOrder());
            
            for (Integer year : sortedYears) {
                AlumniResponse response = new AlumniResponse(year, alumniMap.get(year));
                result.add(response);
            }
            
            logger.info("成功生成往届部员信息，共 {} 个年份", result.size());
            
            // 记录最终结果用于调试
            if (logger.isDebugEnabled()) {
                logger.debug("最终结果:");
                for (AlumniResponse response : result) {
                    logger.debug("年份: {}, 部员数量: {}", response.getYear(), response.getMembers().size());
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