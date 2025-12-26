package com.redmoon2333.service;

import com.redmoon2333.dto.AlumniMember;
import com.redmoon2333.dto.AlumniResponse;
import com.redmoon2333.dto.PublicUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tool Calling服务
 * 提供AI可调用的工具方法，用于数据库查询
 */
@Service
public class ToolService {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolService.class);
    
    @Autowired
    private UserService userService;
    
    /**
     * 工具1：根据姓名搜索部门成员
     * AI可以调用此方法查询人力资源中心的成员信息
     * 
     * @param name 成员姓名或姓名关键词
     * @return 成员信息列表的JSON格式字符串
     */
    @Description("根据姓名查询人力资源中心历史或当前成员信息")
    public String searchDepartmentMembers(String name) {
        logger.info("Tool Calling: 搜索部门成员，姓名: {}", name);
        
        try {
            List<PublicUserInfo> users = userService.searchUsersByNameLike(name);
            
            if (users.isEmpty()) {
                return String.format("未找到姓名包含'%s'的成员", name);
            }
            
            // 格式化结果
            StringBuilder result = new StringBuilder();
            result.append(String.format("找到%d位成员：\n", users.size()));
            
            for (PublicUserInfo user : users) {
                result.append(String.format("- 姓名：%s，角色历史：%s\n", 
                    user.getName(), 
                    user.getRoleHistory() != null ? user.getRoleHistory() : "无"));
            }
            
            logger.info("Tool Calling: 搜索完成，找到{}位成员", users.size());
            return result.toString();
            
        } catch (Exception e) {
            logger.error("Tool Calling: 搜索部门成员失败", e);
            return "查询失败：" + e.getMessage();
        }
    }
    
    /**
     * 工具2：获取往届成员列表
     * AI可以调用此方法查询指定年份或所有年份的往届成员
     * 
     * @param year 年份，如果为null或0则返回所有年份
     * @return 往届成员信息的JSON格式字符串
     */
    @Description("获取指定年份或所有年份的往届成员信息")
    public String getAlumniByYear(Integer year) {
        logger.info("Tool Calling: 获取往届成员，年份: {}", year);
        
        try {
            List<AlumniResponse> allAlumni = userService.getAlumniMembers();
            
            if (allAlumni.isEmpty()) {
                return "暂无往届成员信息";
            }
            
            StringBuilder result = new StringBuilder();
            
            if (year != null && year > 0) {
                // 查询指定年份
                AlumniResponse targetYear = allAlumni.stream()
                    .filter(alumni -> alumni.getYear().equals(year))
                    .findFirst()
                    .orElse(null);
                
                if (targetYear == null) {
                    return String.format("%d年暂无成员记录", year);
                }
                
                result.append(String.format("%d级成员（共%d人）：\n", 
                    targetYear.getYear(), 
                    targetYear.getMembers().size()));
                
                for (AlumniMember member : targetYear.getMembers()) {
                    result.append(String.format("- %s：%s\n", 
                        member.getName(), 
                        member.getRole()));
                }
                
            } else {
                // 返回所有年份
                result.append("往届成员列表：\n");
                for (AlumniResponse alumni : allAlumni) {
                    result.append(String.format("\n【%d级】（%d人）\n", 
                        alumni.getYear(), 
                        alumni.getMembers().size()));
                    
                    for (AlumniMember member : alumni.getMembers()) {
                        result.append(String.format("- %s：%s\n", 
                            member.getName(), 
                            member.getRole()));
                    }
                }
            }
            
            logger.info("Tool Calling: 获取往届成员完成");
            return result.toString();
            
        } catch (Exception e) {
            logger.error("Tool Calling: 获取往届成员失败", e);
            return "查询失败：" + e.getMessage();
        }
    }
    
    /**
     * 工具3：统计部门成员信息
     * AI可以调用此方法获取部门的统计信息
     * 
     * @return 统计信息的字符串
     */
    @Description("获取人力资源中心的成员统计信息")
    public String getDepartmentStats() {
        logger.info("Tool Calling: 获取部门统计信息");
        
        try {
            List<AlumniResponse> allAlumni = userService.getAlumniMembers();
            
            int totalYears = allAlumni.size();
            int totalMembers = allAlumni.stream()
                .mapToInt(alumni -> alumni.getMembers().size())
                .sum();
            
            StringBuilder result = new StringBuilder();
            result.append("人力资源中心统计信息：\n");
            result.append(String.format("- 历史年份：%d届\n", totalYears));
            result.append(String.format("- 历史成员总数：%d人\n", totalMembers));
            
            if (!allAlumni.isEmpty()) {
                result.append(String.format("- 最早记录：%d级\n", 
                    allAlumni.get(allAlumni.size() - 1).getYear()));
                result.append(String.format("- 最新记录：%d级\n", 
                    allAlumni.get(0).getYear()));
            }
            
            logger.info("Tool Calling: 统计信息获取完成");
            return result.toString();
            
        } catch (Exception e) {
            logger.error("Tool Calling: 获取统计信息失败", e);
            return "查询失败：" + e.getMessage();
        }
    }
}
