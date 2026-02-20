package com.redmoon2333.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Markdown格式处理工具（简化版）
 *
 * Why: 遵循markdown-it最佳实践，后端不做任何格式转换
 *      所有Markdown渲染由前端markdown-it处理，后端仅负责流式缓冲管理
 *
 * 核心职责：
 * 1. 流式内容的缓冲区管理（确保完整行被发送）
 * 2. 不做任何格式转换或预处理
 *
 * Reference: https://markdown-it.docschina.org/api/MarkdownIt.html
 */
public class MarkdownFormatter {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownFormatter.class);

    /**
     * 透传Markdown内容（不做任何处理）
     *
     * Why: markdown-it已能正确处理各种Markdown格式，后端预处理是多余的
     *
     * @param content AI原始输出
     * @return 原样返回内容
     */
    public static String format(String content) {
        return content;
    }

    /**
     * 流式内容的缓冲区管理
     * 用于流式响应，确保只发送完整的行
     *
     * Why: 流式响应需要按行发送，避免发送不完整的Markdown语法
     *      例如：**加粗** 被拆分成多个chunk时，需要等待完整
     *
     * @param chunk 当前数据块
     * @param buffer 缓冲区（用于存储不完整的行）
     * @return 完整行的内容
     */
    public static String formatStreamChunk(String chunk, StringBuilder buffer) {
        if (chunk == null) {
            return "";
        }

        buffer.append(chunk);

        int lastNewline = buffer.lastIndexOf("\n");
        if (lastNewline == -1) {
            if (buffer.length() > 100) {
                String content = buffer.toString();
                buffer.setLength(0);
                return content;
            }
            return "";
        }

        String completeLines = buffer.substring(0, lastNewline + 1);
        String remaining = buffer.substring(lastNewline + 1);

        buffer.setLength(0);
        buffer.append(remaining);

        return completeLines;
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
        return remaining;
    }
}
