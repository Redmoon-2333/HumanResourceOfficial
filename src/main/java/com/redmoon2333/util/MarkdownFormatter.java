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
     * @param content AI原始输出
     * @return 格式化后的内容
     */
    public static String format(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String original = content;

        // 只修复明确的格式问题，避免过度处理
        // 1. 将4个或以上的"-"替换为标准的分割线"---"
        content = content.replaceAll("-{4,}", "---");

        if (!original.equals(content)) {
            logger.debug("Markdown格式已清理");
        }

        return content;
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
