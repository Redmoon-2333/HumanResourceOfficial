package com.redmoon2333.service;

import com.redmoon2333.dto.AlumniMember;
import com.redmoon2333.dto.AlumniResponse;
import com.redmoon2333.dto.PastActivityResponse;
import com.redmoon2333.dto.PublicUserInfo;
import com.redmoon2333.entity.Activity;
import com.redmoon2333.entity.Material;
import com.redmoon2333.entity.PastActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI工具服务
 * 使用 Spring AI 的 @Tool 注解，AI模型会自动识别并决定何时调用这些工具
 * 无需手动关键词匹配或意图识别
 */
@Service
public class ToolService {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolService.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private MaterialService materialService;
    
    @Autowired
    private ActivityService activityService;
    
    @Autowired
    private PastActivityService pastActivityService;
    
    /**
     * 根据姓名搜索部门成员
     */
    @Tool(description = "根据姓名查询人力资源中心的历史或当前成员信息，用于查找特定人员")
    public String searchDepartmentMembers(
            @ToolParam(description = "要搜索的成员姓名或姓名关键词") String name) {
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
     * 获取往届成员列表
     */
    @Tool(description = "获取人力资源中心的成员列表，可指定年份查询或获取所有年份的成员。用于回答'部门有哪些人'、'成员是谁'、'有2023级的成员'等问题")
    public String getAlumniByYear(
            @ToolParam(description = "年份，如果为null或0则返回所有年份的成员") Integer year) {
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
     * 统计部门成员信息
     */
    @Tool(description = "获取人力资源中心的成员统计信息，包括总人数、届数等。用于回答'部门有多少人'、'人数统计'等问题")
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
    
    /**
     * 全局检索 - 搜索成员、资料、活动、往届活动
     * 用于回答"搜索xxx"、"查找xxx"、"查询xxx"等通用搜索问题
     */
    @Tool(description = "全局检索功能，可以在人力资源中心系统中搜索成员、资料、活动和往届活动。支持同时搜索多个类型，或指定特定类型搜索。用于回答'搜索张三'、'查找活动策划资料'、'查询2023年的活动'等问题。")
    public String globalSearch(
            @ToolParam(description = "搜索关键词，必填") String keyword,
            @ToolParam(description = "搜索类型，可选值：member(成员)、material(资料)、activity(活动)、past_activity(往届活动)、all(全部，默认)") String searchType,
            @ToolParam(description = "每类结果返回的最大数量，默认5条") Integer limit) {
        
        logger.info("Tool Calling: 全局检索，关键词: {}, 类型: {}", keyword, searchType);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return "请提供搜索关键词";
        }
        
        if (limit == null || limit <= 0) {
            limit = 5;
        }
        
        final int finalLimit = limit;
        StringBuilder result = new StringBuilder();
        boolean hasResults = false;
        
        try {
            // 搜索成员
            if ("all".equalsIgnoreCase(searchType) || "member".equalsIgnoreCase(searchType)) {
                List<PublicUserInfo> members = userService.searchUsersByNameLike(keyword);
                if (!members.isEmpty()) {
                    hasResults = true;
                    result.append("👤 成员搜索结果（共").append(members.size()).append("条）：\n");
                    List<PublicUserInfo> limitedMembers = members.stream().limit(finalLimit).collect(Collectors.toList());
                    for (PublicUserInfo member : limitedMembers) {
                        result.append(String.format("  - %s（角色：%s）\n", 
                            member.getName(), 
                            member.getRoleHistory() != null ? member.getRoleHistory() : "无"));
                    }
                    if (members.size() > finalLimit) {
                        result.append(String.format("  ... 还有%d条结果\n", members.size() - finalLimit));
                    }
                }
            }
            
            // 搜索资料
            if ("all".equalsIgnoreCase(searchType) || "material".equalsIgnoreCase(searchType)) {
                List<Material> materials = materialService.searchMaterialsByName(keyword);
                if (!materials.isEmpty()) {
                    hasResults = true;
                    result.append("\n📚 资料搜索结果（共").append(materials.size()).append("条）：\n");
                    List<Material> limitedMaterials = materials.stream().limit(finalLimit).collect(Collectors.toList());
                    for (Material material : limitedMaterials) {
                        String desc = material.getDescription();
                        String descText = desc != null && !desc.isEmpty() ? " - " + (desc.length() > 30 ? desc.substring(0, 30) + "..." : desc) : "";
                        result.append(String.format("  - %s%s\n", material.getMaterialName(), descText));
                    }
                    if (materials.size() > finalLimit) {
                        result.append(String.format("  ... 还有%d条结果\n", materials.size() - finalLimit));
                    }
                }
            }
            
            // 搜索当前活动
            if ("all".equalsIgnoreCase(searchType) || "activity".equalsIgnoreCase(searchType)) {
                List<Activity> activities = activityService.getAllActivities();
                List<Activity> matchedActivities = activities.stream()
                    .filter(a -> (a.getActivityName() != null && a.getActivityName().contains(keyword)) ||
                                (a.getPurpose() != null && a.getPurpose().contains(keyword)) ||
                                (a.getSignificance() != null && a.getSignificance().contains(keyword)))
                    .collect(Collectors.toList());
                
                if (!matchedActivities.isEmpty()) {
                    hasResults = true;
                    result.append("\n🎪 当前活动搜索结果（共").append(matchedActivities.size()).append("条）：\n");
                    List<Activity> limitedActivities = matchedActivities.stream().limit(finalLimit).collect(Collectors.toList());
                    for (Activity activity : limitedActivities) {
                        result.append(String.format("  - %s\n", activity.getActivityName()));
                    }
                    if (matchedActivities.size() > finalLimit) {
                        result.append(String.format("  ... 还有%d条结果\n", matchedActivities.size() - finalLimit));
                    }
                }
            }
            
            // 搜索往届活动
            if ("all".equalsIgnoreCase(searchType) || "past_activity".equalsIgnoreCase(searchType) || "pastactivity".equalsIgnoreCase(searchType)) {
                List<PastActivityResponse> pastActivities = pastActivityService.getPagedPastActivities(1, 100, null, keyword).getContent();
                if (!pastActivities.isEmpty()) {
                    hasResults = true;
                    result.append("\n📜 往届活动搜索结果（共").append(pastActivities.size()).append("条）：\n");
                    List<PastActivityResponse> limitedPast = pastActivities.stream().limit(finalLimit).collect(Collectors.toList());
                    for (PastActivityResponse past : limitedPast) {
                        result.append(String.format("  - %s（%d年）\n", past.getTitle(), past.getYear()));
                    }
                    if (pastActivities.size() > finalLimit) {
                        result.append(String.format("  ... 还有%d条结果\n", pastActivities.size() - finalLimit));
                    }
                }
            }
            
            // 处理无结果的情况
            if (!hasResults) {
                return String.format("未找到与「%s」相关的任何结果", keyword);
            }
            
            logger.info("Tool Calling: 全局检索完成，找到结果");
            return result.toString();
            
        } catch (Exception e) {
            logger.error("Tool Calling: 全局检索失败", e);
            return "检索失败：" + e.getMessage();
        }
    }
    
    /**
     * 快速搜索 - 简化版全局搜索
     */
    @Tool(description = "快速搜索功能，只需输入关键词即可搜索成员、资料、活动、往届活动等所有内容。用于回答'搜索'、'查找'等简单查询")
    public String quickSearch(
            @ToolParam(description = "搜索关键词") String keyword) {
        return globalSearch(keyword, "all", 5);
    }
}
