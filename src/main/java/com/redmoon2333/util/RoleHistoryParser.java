package com.redmoon2333.util;

import com.redmoon2333.entity.User;
import com.redmoon2333.exception.BusinessException;
import com.redmoon2333.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoleHistoryParser {

    private static final Pattern ROLE_PATTERN = Pattern.compile("(\\d{4})级(.+)");

    public static ParsedRole getLatestRole(User user) {
        if (user.getRoleHistory() == null || user.getRoleHistory().isBlank()) {
            throw new BusinessException(ErrorCode.ROLE_HISTORY_PARSE_ERROR, "用户无角色历史");
        }

        List<String> entries = parseRoleHistory(user.getRoleHistory());
        if (entries.isEmpty()) {
            throw new BusinessException(ErrorCode.ROLE_HISTORY_PARSE_ERROR, "角色历史解析失败: " + user.getRoleHistory());
        }

        String lastEntry = entries.get(entries.size() - 1);
        return parseSingleRole(lastEntry);
    }

    public static ParsedRole parseSingleRole(String entry) {
        Matcher matcher = ROLE_PATTERN.matcher(entry.trim());
        if (!matcher.matches()) {
            throw new BusinessException(ErrorCode.ROLE_HISTORY_PARSE_ERROR, "角色条目格式不匹配: " + entry);
        }
        int year = Integer.parseInt(matcher.group(1));
        String role = matcher.group(2).trim();
        return new ParsedRole(year, role);
    }

    public static boolean isCurrentMinisterOrDeputy(User user, int currentYear) {
        ParsedRole latest = getLatestRole(user);
        // 先匹配副部长，再匹配部长（避免"副部长"被误判）
        return latest.year() == currentYear && (latest.role().contains("副部长") || latest.role().endsWith("部长"));
    }

    public static boolean isCurrentMember(User user, int currentYear) {
        ParsedRole latest = getLatestRole(user);
        // 使用 endsWith 精确匹配"部员"，避免匹配到"副部长"
        return latest.year() == currentYear && latest.role().endsWith("部员");
    }

    /**
     * 追加新角色到角色历史
     * 写入时统一使用新格式：2024级部员、2024级副部长、2024级部长
     * 不添加多余空格或JSON数组包装
     */
    public static String appendRole(String roleHistory, int year, String newRole) {
        String newEntry = year + "级" + newRole;
        if (roleHistory == null || roleHistory.isBlank()) {
            return newEntry;
        }

        List<String> existingEntries = parseRoleHistory(roleHistory);

        if (existingEntries.isEmpty()) {
            return newEntry;
        }

        for (String entry : existingEntries) {
            if (entry.equals(newEntry)) {
                return roleHistory;
            }
        }

        String lastEntry = existingEntries.get(existingEntries.size() - 1);
        return lastEntry + "&" + newEntry;
    }

    /**
     * 解析角色历史字符串
     * 读取时兼容旧格式，输出时统一为无空格格式
     * 支持格式：
     * - 新格式: 2024级部员&2025级部长
     * - 旧格式: ["2024级部员"]、2024 级部长、["2024 级部长"]
     */
    public static List<String> parseRoleHistory(String roleHistory) {
        List<String> result = new ArrayList<>();
        if (roleHistory == null || roleHistory.trim().isEmpty()) {
            return result;
        }

        String cleaned = roleHistory.trim();

        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
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

            for (String element : jsonElements) {
                element = element.replace("\"", "").replace("'", "").trim();
                element = element.replace(" ", "");
                if (element.contains("&")) {
                    String[] parts = element.split("&");
                    for (String part : parts) {
                        part = part.trim().replace(" ", "");
                        if (!part.isEmpty()) {
                            result.add(part);
                        }
                    }
                } else if (!element.isEmpty()) {
                    result.add(element);
                }
            }
            return result;
        }

        cleaned = cleaned.replace("[", "").replace("]", "").replace("\"", "").replace("'", "").replace(" ", "").trim();
        String[] parts = cleaned.split("[&,]");
        for (String part : parts) {
            part = part.trim().replace(" ", "");
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }

    public record ParsedRole(int year, String role) {
    }
}
