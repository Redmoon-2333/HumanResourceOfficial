package com.redmoon2333.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块工具类
 * 负责将长文本切分为适当大小的块
 */
public class TextChunker {
    
    private static final Logger logger = LoggerFactory.getLogger(TextChunker.class);
    
    /**
     * 句子结束标记
     */
    private static final String[] SENTENCE_ENDINGS = {"。", "！", "？", ".", "!", "?"};
    
    /**
     * 段落分隔符
     */
    private static final String PARAGRAPH_SEPARATOR = "\n\n";
    
    /**
     * 将文本分块
     * 
     * @param text 原始文本
     * @param chunkSize 分块大小
     * @param chunkOverlap 重叠大小
     * @return 分块后的文本列表
     */
    public static List<String> chunkText(String text, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        // 预处理文本：规范化空白符
        text = normalizeText(text);
        
        int textLength = text.length();
        int start = 0;
        
        logger.debug("开始分块，文本长度: {}, 分块大小: {}, 重叠大小: {}", 
                    textLength, chunkSize, chunkOverlap);
        
        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            
            // 如果不是最后一块，尝试在句子边界处切分
            if (end < textLength) {
                end = findBestSplitPoint(text, start, end);
            }
            
            String chunk = text.substring(start, end).trim();
            
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
                logger.trace("添加分块 {}: 长度 {}", chunks.size(), chunk.length());
            }
            
            // 计算下一块的起始位置，考虑重叠
            start = end - chunkOverlap;
            if (start <= 0 || start >= textLength) {
                break;
            }
        }
        
        logger.debug("分块完成，共 {} 块", chunks.size());
        return chunks;
    }
    
    /**
     * 规范化文本
     * 去除多余的空白符，统一换行符
     * 
     * @param text 原始文本
     * @return 规范化后的文本
     */
    private static String normalizeText(String text) {
        // 统一换行符
        text = text.replace("\r\n", "\n").replace("\r", "\n");
        
        // 移除行首行尾空白
        String[] lines = text.split("\n");
        StringBuilder normalized = new StringBuilder();
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                normalized.append(trimmed).append("\n");
            }
        }
        
        return normalized.toString();
    }
    
    /**
     * 查找最佳分割点
     * 优先在句子边界、段落边界处切分
     * 
     * @param text 文本
     * @param start 起始位置
     * @param end 建议结束位置
     * @return 实际结束位置
     */
    private static int findBestSplitPoint(String text, int start, int end) {
        // 在建议结束位置附近查找（前后50个字符范围内）
        int searchStart = Math.max(start, end - 50);
        int searchEnd = Math.min(text.length(), end + 50);
        
        // 1. 优先查找段落分隔符
        int paragraphEnd = text.lastIndexOf(PARAGRAPH_SEPARATOR, searchEnd);
        if (paragraphEnd > searchStart) {
            return paragraphEnd + PARAGRAPH_SEPARATOR.length();
        }
        
        // 2. 查找句子结束标记
        int sentenceEnd = -1;
        for (String ending : SENTENCE_ENDINGS) {
            int pos = text.lastIndexOf(ending, searchEnd);
            if (pos > searchStart && pos > sentenceEnd) {
                sentenceEnd = pos + ending.length();
            }
        }
        
        if (sentenceEnd > searchStart) {
            return sentenceEnd;
        }
        
        // 3. 查找空格
        int spacePos = text.lastIndexOf(' ', end);
        if (spacePos > searchStart) {
            return spacePos + 1;
        }
        
        // 4. 如果都找不到，就在原位置切分
        return end;
    }
    
    /**
     * 计算文本的MD5哈希值
     * 
     * @param text 文本内容
     * @return MD5哈希值（十六进制字符串）
     */
    public static String calculateMD5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(text.getBytes());
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("MD5算法不可用", e);
            throw new RuntimeException("MD5计算失败", e);
        }
    }
}
