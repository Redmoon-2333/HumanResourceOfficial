package com.redmoon2333.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Markdown格式清理工具
 *
 * Why: AI有时会忘记格式规范，在列表项内使用"-"作为分隔符，导致渲染混乱
 * 此类作为后处理兜底机制，在AI响应返回前端前自动纠正格式问题
 */
public class MarkdownFormatter {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownFormatter.class);

    /**
     * 清理并格式化AI输出的Markdown内容
     *
     * 主要修复问题：
     * 1. 列表项内使用"-"分隔多个主题的问题
     * 2. 将"-"分隔的内容拆分为独立的列表项
     *
     * @param content AI原始输出
     * @return 格式化后的内容
     */
    public static String format(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String original = content;

        // 修复：列表项内使用" - "（空格-空格）分隔多个主题的问题
        // 例如：- **活动组织**：xxx - **团队协作**：xxx
        // 修复为：
        // - **活动组织**：xxx
        //
        // - **团队协作**：xxx
        content = fixListItemSeparators(content);

        // 修复：列表项内使用"-"（无空格）分隔的问题
        content = fixListItemSeparatorsWithoutSpaces(content);

        // 修复：多个连续的"-"导致的问题
        content = fixConsecutiveDashes(content);

        if (!original.equals(content)) {
            logger.debug("Markdown格式已清理");
        }

        return content;
    }

    /**
     * 修复列表项内使用" - "分隔的问题
     *
     * 匹配模式：以"- "开头的一行中包含" - "（用于分隔主题）
     * 注意：要排除合法的列表开头"- "
     */
    private static String fixListItemSeparators(String content) {
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");

        for (String line : lines) {
            // 检查是否是列表项行（以"- "开头）
            if (line.trim().startsWith("- ")) {
                String fixedLine = fixSingleListItem(line);
                result.append(fixedLine);
            } else {
                result.append(line);
            }
            result.append("\n");
        }

        // 移除末尾多余的换行
        if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
            result.setLength(result.length() - 1);
        }

        return result.toString();
    }

    /**
     * 修复单行列表项内的分隔符问题
     *
     * 将：- **活动组织**：xxx - **团队协作**：xxx
     * 改为：- **活动组织**：xxx\n\n- **团队协作**：xxx
     */
    private static String fixSingleListItem(String line) {
        // 找到第一个"- "（列表标记）之后的内容
        int firstDashIndex = line.indexOf("- ");
        if (firstDashIndex == -1) {
            return line;
        }

        String beforeFirstDash = line.substring(0, firstDashIndex);
        String afterFirstDash = line.substring(firstDashIndex + 2); // 跳过"- "

        // 在剩余内容中查找" - "（用于分隔主题的）
        // 需要确保这个"-"不是列表的一部分（前面不是行首）
        StringBuilder result = new StringBuilder();
        result.append(beforeFirstDash).append("- ");

        String remaining = afterFirstDash;
        boolean firstPart = true;

        while (true) {
            int separatorIndex = findSeparatorIndex(remaining);
            if (separatorIndex == -1) {
                // 没有更多分隔符了
                if (!firstPart) {
                    result.append("\n").append(beforeFirstDash).append("- ");
                }
                result.append(remaining);
                break;
            }

            // 提取分隔符前的内容
            String part = remaining.substring(0, separatorIndex).trim();
            if (!firstPart) {
                result.append("\n").append(beforeFirstDash).append("- ");
            }
            result.append(part);

            // 继续处理分隔符后的内容
            remaining = remaining.substring(separatorIndex + 3).trim(); // 跳过" - "
            firstPart = false;
        }

        return result.toString();
    }

    /**
     * 查找分隔符" - "的位置
     * 需要确保这个"-"不是代码块、链接或其他特殊语法的一部分
     */
    private static int findSeparatorIndex(String text) {
        int index = 0;
        while (true) {
            int found = text.indexOf(" - ", index);
            if (found == -1) {
                return -1;
            }

            // 检查这个"-"是否在加粗文本内（**...**）
            if (isInsideBold(text, found)) {
                index = found + 1;
                continue;
            }

            // 检查这个"-"前面是否是列表项的开头模式
            // 例如："xx - **标题" 这种是分隔符
            // 但 "- **标题" 这种是列表开头，不应该在这里出现
            return found;
        }
    }

    /**
     * 检查指定位置是否在加粗文本内
     */
    private static boolean isInsideBold(String text, int position) {
        int boldCount = 0;
        for (int i = 0; i < position; i++) {
            if (i + 1 < text.length() && text.charAt(i) == '*' && text.charAt(i + 1) == '*') {
                boldCount++;
                i++; // 跳过下一个*
            }
        }
        // 如果boldCount是奇数，说明在加粗文本内
        return boldCount % 2 == 1;
    }

    /**
     * 修复列表项内使用"-"（无空格）分隔的问题
     */
    private static String fixListItemSeparatorsWithoutSpaces(String content) {
        // 这种情况比较少见，先简单处理
        // 例如：- 活动组织：xxx-团队协作：xxx
        // 这种格式不太常见，暂时不处理复杂逻辑
        return content;
    }

    /**
     * 修复连续的"---"问题
     */
    private static String fixConsecutiveDashes(String content) {
        // 将4个或以上的"-"替换为标准的分割线"---"
        return content.replaceAll("-{4,}", "---");
    }

    /**
     * 流式内容的增量格式化
     * 用于流式响应，只处理完整的行
     *
     * @param chunk 当前数据块
     * @param buffer 缓冲区（用于存储不完整的行）
     * @return 格式化后的完整内容
     */
    public static String formatStreamChunk(String chunk, StringBuilder buffer) {
        if (chunk == null) {
            return "";
        }

        buffer.append(chunk);

        // 找到最后一个换行符
        int lastNewline = buffer.lastIndexOf("\n");
        if (lastNewline == -1) {
            // 没有完整行，返回空，继续缓冲
            return "";
        }

        // 提取完整的行进行格式化
        String completeLines = buffer.substring(0, lastNewline + 1);
        String remaining = buffer.substring(lastNewline + 1);

        // 清空缓冲区并保留未完成的行
        buffer.setLength(0);
        buffer.append(remaining);

        // 格式化完整的行
        return format(completeLines);
    }

    /**
     * 刷新流式缓冲区的剩余内容
     */
    public static String flushStreamBuffer(StringBuilder buffer) {
        if (buffer.length() == 0) {
            return "";
        }
        String remaining = buffer.toString();
        buffer.setLength(0);
        return format(remaining);
    }
}
