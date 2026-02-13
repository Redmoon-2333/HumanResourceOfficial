package com.redmoon2333.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能文本分块器 v2.0
 * 针对RAG知识库优化的语义分块策略
 * 
 * 优化特性：
 * - 按语义边界分块（章节、段落、句子）
 * - 识别中文标题层级（一、二、三 和 【】标记）
 * - 内存优化：流式处理，避免大文本一次性加载
 * - 智能合并：小段落合并，避免过度碎片化
 * 
 * Why: 不同类型的文档需要不同的分块策略
 * - 结构化文档（如规章制度）：按章节分块，保持语义完整
 * - 叙述性文档（如活动介绍）：中等分块保持语义完整性
 * - 技术文档（如操作指南）：大分块保留完整操作步骤
 * 
 * Warning: 分块大小过大会降低检索精度，过小会丢失上下文
 */
public class SmartTextChunker {
    
    private static final Logger logger = LoggerFactory.getLogger(SmartTextChunker.class);
    
    // ============================================================
    // 语义边界识别模式
    // ============================================================
    
    private static final String[] SENTENCE_ENDINGS = {"。", "！", "？", ".", "!", "?"};
    private static final String PARAGRAPH_SEPARATOR = "\n\n";
    
    // 中文章节标题模式：一、二、三... 或 1. 2. 3...
    private static final Pattern CHINESE_HEADING_PATTERN = Pattern.compile(
        "^[一二三四五六七八九十百]+[、．.]\\s*.+|^\\d+[、．.]\\s*.+", Pattern.MULTILINE);
    
    // 【】标记的小节标题
    private static final Pattern BRACKET_HEADING_PATTERN = Pattern.compile(
        "^【[^】]+】", Pattern.MULTILINE);
    
    // Markdown标题
    private static final Pattern MARKDOWN_HEADING_PATTERN = Pattern.compile(
        "^#{1,6}\\s+.+", Pattern.MULTILINE);
    
    // 列表项模式
    private static final Pattern LIST_PATTERN = Pattern.compile(
        "^[\\d一二三四五六七八九十]+[、.．].+|^[-*]\\s+.+", Pattern.MULTILINE);
    
    private static final Pattern CODE_PATTERN = Pattern.compile("```[\\s\\S]*?```|`[^`]+`");
    private static final Pattern TABLE_PATTERN = Pattern.compile("\\|.+\\|\\n\\|[-:| ]+\\|");
    
    public enum DocumentType {
        STRUCTURED,    // 结构化文档（规章制度、部门介绍）
        NARRATIVE,     // 叙述性文档（活动介绍、指南）
        TECHNICAL,     // 技术文档（操作指南、代码文档）
        KNOWLEDGE_BASE // 知识库文档（校园信息、FAQ）
    }
    
    public static class ChunkConfig {
        private final int chunkSize;
        private final int chunkOverlap;
        private final DocumentType documentType;
        
        public ChunkConfig(int chunkSize, int chunkOverlap, DocumentType documentType) {
            this.chunkSize = chunkSize;
            this.chunkOverlap = chunkOverlap;
            this.documentType = documentType;
        }
        
        public int getChunkSize() { return chunkSize; }
        public int getChunkOverlap() { return chunkOverlap; }
        public DocumentType getDocumentType() { return documentType; }
    }
    
    /**
     * 分析文档类型
     * 根据文档特征自动判断文档类型
     * 
     * 优化：增加对知识库文档的识别
     * 
     * @param text 文档文本
     * @return 文档类型
     */
    public static DocumentType analyzeDocumentType(String text) {
        if (text == null || text.isEmpty()) {
            return DocumentType.NARRATIVE;
        }
        
        int textLength = text.length();
        
        // 统计各类特征
        int listMatches = countMatches(LIST_PATTERN, text);
        int chineseHeadingMatches = countMatches(CHINESE_HEADING_PATTERN, text);
        int bracketHeadingMatches = countMatches(BRACKET_HEADING_PATTERN, text);
        int markdownHeadingMatches = countMatches(MARKDOWN_HEADING_PATTERN, text);
        int codeMatches = countMatches(CODE_PATTERN, text);
        int tableMatches = countMatches(TABLE_PATTERN, text);
        
        int totalHeadingMatches = chineseHeadingMatches + bracketHeadingMatches + markdownHeadingMatches;
        
        // 计算各类得分（归一化处理）
        double normalizer = textLength / 500.0 + 1;
        double structureScore = (listMatches + totalHeadingMatches) * 100.0 / normalizer;
        double technicalScore = (codeMatches + tableMatches) * 100.0 / normalizer;
        double knowledgeBaseScore = bracketHeadingMatches * 150.0 / normalizer; // 【】标记是知识库特征
        
        logger.debug("文档分析 - 结构化得分: {:.2f}, 技术得分: {:.2f}, 知识库得分: {:.2f}", 
                    structureScore, technicalScore, knowledgeBaseScore);
        
        // 优先级：技术文档 > 知识库文档 > 结构化文档 > 叙述性文档
        if (technicalScore > 5) {
            return DocumentType.TECHNICAL;
        } else if (knowledgeBaseScore > 4) {
            return DocumentType.KNOWLEDGE_BASE;
        } else if (structureScore > 3) {
            return DocumentType.STRUCTURED;
        } else {
            return DocumentType.NARRATIVE;
        }
    }
    
    /**
     * 根据文档类型获取推荐配置
     */
    public static ChunkConfig getRecommendedConfig(String text) {
        return getRecommendedConfig(text, analyzeDocumentType(text));
    }
    
    /**
     * 根据文档类型获取推荐配置
     * 
     * 优化说明：
     * - STRUCTURED：小分块(300)提高关键词密度，适合精确检索
     * - NARRATIVE：中等分块(500)保持语义完整性
     * - TECHNICAL：大分块(800)保留完整操作步骤
     * - KNOWLEDGE_BASE：中小分块(400)平衡检索精度和语义完整性
     */
    public static ChunkConfig getRecommendedConfig(String text, DocumentType documentType) {
        return switch (documentType) {
            case STRUCTURED -> new ChunkConfig(300, 80, DocumentType.STRUCTURED);
            case TECHNICAL -> new ChunkConfig(800, 200, DocumentType.TECHNICAL);
            case KNOWLEDGE_BASE -> new ChunkConfig(400, 100, DocumentType.KNOWLEDGE_BASE);
            default -> new ChunkConfig(500, 120, DocumentType.NARRATIVE);
        };
    }
    
    private static int countMatches(Pattern pattern, String text) {
        return pattern.matcher(text).results().mapToInt(r -> 1).sum();
    }
    
    /**
     * 智能分块
     * 自动分析文档类型并选择最佳分块策略
     * 
     * @param text 原始文本
     * @return 分块后的文本列表
     */
    public static List<String> smartChunk(String text) {
        DocumentType docType = analyzeDocumentType(text);
        ChunkConfig config = getRecommendedConfig(text, docType);
        
        logger.info("智能分块 - 文档类型: {}, 分块大小: {}, 重叠大小: {}", 
                   docType, config.getChunkSize(), config.getChunkOverlap());
        
        return chunkText(text, config.getChunkSize(), config.getChunkOverlap());
    }
    
    /**
     * 智能分块（指定配置）
     * 
     * @param text 原始文本
     * @param config 分块配置
     * @return 分块后的文本列表
     */
    public static List<String> smartChunk(String text, ChunkConfig config) {
        return chunkText(text, config.getChunkSize(), config.getChunkOverlap());
    }
    
    /**
     * 将文本分块（基础方法）
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
        
        text = normalizeText(text);
        
        int textLength = text.length();
        int start = 0;
        
        logger.debug("开始分块，文本长度: {}, 分块大小: {}, 重叠大小: {}", 
                    textLength, chunkSize, chunkOverlap);
        
        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);
            
            if (end < textLength) {
                end = findBestSplitPoint(text, start, end, chunkSize);
            }
            
            String chunk = text.substring(start, end).trim();
            
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
                logger.trace("添加分块 {}: 长度 {}", chunks.size(), chunk.length());
            }
            
            start = end - chunkOverlap;
            if (start <= 0 || start >= textLength) {
                break;
            }
            
            start = Math.max(start, 0);
        }
        
        logger.debug("分块完成，共 {} 块", chunks.size());
        return chunks;
    }
    
    // ============================================================
    // 语义分块方法（v2.0新增）
    // ============================================================
    
    /**
     * 语义分块 - 按章节和段落边界智能分割
     * 
     * 优化策略：
     * 1. 首先按章节标题（一、二、三 或 【】）分割
     * 2. 对于过长的章节，按段落边界二次分割
     * 3. 对于过短的段落，智能合并
     * 4. 确保每个块的语义完整性
     * 
     * @param text 原始文本
     * @param maxChunkSize 最大分块大小
     * @param minChunkSize 最小分块大小（过小的块会被合并）
     * @param chunkOverlap 重叠大小
     * @return 分块后的文本列表
     */
    public static List<String> semanticChunk(String text, int maxChunkSize, int minChunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        text = normalizeText(text);
        
        // 第一步：按主要章节标题分割
        List<String> sections = splitBySections(text);
        
        logger.debug("语义分块 - 识别到 {} 个章节", sections.size());
        
        // 第二步：处理每个章节
        StringBuilder pendingChunk = new StringBuilder();
        
        for (String section : sections) {
            // 如果当前待处理块 + 新章节 <= maxChunkSize，合并
            if (pendingChunk.length() + section.length() <= maxChunkSize) {
                if (pendingChunk.length() > 0) {
                    pendingChunk.append("\n\n");
                }
                pendingChunk.append(section);
            } else {
                // 先保存当前待处理块
                if (pendingChunk.length() >= minChunkSize) {
                    chunks.add(pendingChunk.toString().trim());
                    pendingChunk = new StringBuilder();
                }
                
                // 处理新章节
                if (section.length() <= maxChunkSize) {
                    // 章节大小合适，直接作为一个块或合并到待处理
                    if (pendingChunk.length() + section.length() <= maxChunkSize) {
                        if (pendingChunk.length() > 0) {
                            pendingChunk.append("\n\n");
                        }
                        pendingChunk.append(section);
                    } else {
                        if (pendingChunk.length() > 0) {
                            chunks.add(pendingChunk.toString().trim());
                        }
                        pendingChunk = new StringBuilder(section);
                    }
                } else {
                    // 章节过长，按段落进一步分割
                    if (pendingChunk.length() > 0) {
                        chunks.add(pendingChunk.toString().trim());
                        pendingChunk = new StringBuilder();
                    }
                    List<String> subChunks = splitLongSection(section, maxChunkSize, minChunkSize, chunkOverlap);
                    chunks.addAll(subChunks);
                }
            }
        }
        
        // 处理最后的待处理块
        if (pendingChunk.length() > 0) {
            String finalChunk = pendingChunk.toString().trim();
            if (!finalChunk.isEmpty()) {
                // 如果最后的块太小，尝试合并到前一个块
                if (finalChunk.length() < minChunkSize && !chunks.isEmpty()) {
                    String lastChunk = chunks.remove(chunks.size() - 1);
                    if (lastChunk.length() + finalChunk.length() <= maxChunkSize * 1.2) {
                        chunks.add(lastChunk + "\n\n" + finalChunk);
                    } else {
                        chunks.add(lastChunk);
                        chunks.add(finalChunk);
                    }
                } else {
                    chunks.add(finalChunk);
                }
            }
        }
        
        logger.debug("语义分块完成，共 {} 块", chunks.size());
        return chunks;
    }
    
    /**
     * 按章节标题分割文本
     * 识别中文标题（一、二、三）和【】标记的小节
     */
    private static List<String> splitBySections(String text) {
        List<String> sections = new ArrayList<>();
        
        // 组合所有章节分隔符模式
        // 匹配：一、xxx 或 1. xxx 或 【xxx】（作为新行开头）
        Pattern sectionPattern = Pattern.compile(
            "(?=^[一二三四五六七八九十百]+[、．.])|(?=^\\d+[、．.])|(?=^【[^】]+】)",
            Pattern.MULTILINE
        );
        
        String[] parts = sectionPattern.split(text);
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sections.add(trimmed);
            }
        }
        
        // 如果没有识别到章节，返回整个文本
        if (sections.isEmpty()) {
            sections.add(text.trim());
        }
        
        return sections;
    }
    
    /**
     * 分割过长的章节
     * 按段落和句子边界进行二次分割
     */
    private static List<String> splitLongSection(String section, int maxChunkSize, int minChunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        
        // 按段落分割
        String[] paragraphs = section.split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;
            
            if (currentChunk.length() + paragraph.length() + 2 <= maxChunkSize) {
                // 可以合并
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            } else {
                // 需要分割
                if (currentChunk.length() >= minChunkSize) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                
                if (paragraph.length() <= maxChunkSize) {
                    // 段落大小合适
                    if (currentChunk.length() > 0) {
                        currentChunk.append("\n\n");
                    }
                    currentChunk.append(paragraph);
                } else {
                    // 段落过长，按句子分割
                    if (currentChunk.length() > 0) {
                        chunks.add(currentChunk.toString().trim());
                        currentChunk = new StringBuilder();
                    }
                    List<String> sentenceChunks = splitBySentences(paragraph, maxChunkSize, minChunkSize);
                    chunks.addAll(sentenceChunks);
                }
            }
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    /**
     * 按句子边界分割过长的段落
     */
    private static List<String> splitBySentences(String paragraph, int maxChunkSize, int minChunkSize) {
        List<String> chunks = new ArrayList<>();
        
        // 按句子结束符分割
        Pattern sentencePattern = Pattern.compile("(?<=[。！？.!?])\\s*");
        String[] sentences = sentencePattern.split(paragraph);
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;
            
            if (currentChunk.length() + sentence.length() + 1 <= maxChunkSize) {
                if (currentChunk.length() > 0 && !currentChunk.toString().endsWith("\n")) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            } else {
                if (currentChunk.length() >= minChunkSize) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                
                if (sentence.length() <= maxChunkSize) {
                    currentChunk.append(sentence);
                } else {
                    // 单个句子过长，强制截断
                    if (currentChunk.length() > 0) {
                        chunks.add(currentChunk.toString().trim());
                        currentChunk = new StringBuilder();
                    }
                    // 按固定大小截断超长句子
                    for (int i = 0; i < sentence.length(); i += maxChunkSize - 50) {
                        int end = Math.min(i + maxChunkSize, sentence.length());
                        chunks.add(sentence.substring(i, end).trim());
                    }
                }
            }
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    // ============================================================
    // Spring AI Document 转换方法
    // ============================================================
    
    /**
     * 将文本智能分块并转换为Spring AI Document列表
     * 内存优化：流式处理，避免中间对象堆积
     * 
     * @param text 原始文本
     * @param fileName 文件名（用于元数据）
     * @param maxChunkSize 最大分块大小
     * @param minChunkSize 最小分块大小
     * @param chunkOverlap 重叠大小
     * @return Document列表
     */
    public static List<Document> chunkToDocuments(String text, String fileName, 
                                                   int maxChunkSize, int minChunkSize, int chunkOverlap) {
        List<Document> documents = new ArrayList<>();
        
        // 分析文档类型
        DocumentType docType = analyzeDocumentType(text);
        logger.info("文档 {} 类型识别为: {}", fileName, docType);
        
        // 使用语义分块
        List<String> chunks = semanticChunk(text, maxChunkSize, minChunkSize, chunkOverlap);
        
        // 转换为Document对象
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", fileName);
            metadata.put("chunk_index", i);
            metadata.put("total_chunks", chunks.size());
            metadata.put("doc_type", docType.name());
            
            documents.add(new Document(chunk, metadata));
            
            // 立即释放chunk引用，帮助GC
            chunks.set(i, null);
        }
        
        return documents;
    }
    
    /**
     * 使用自动配置进行分块
     */
    public static List<Document> chunkToDocuments(String text, String fileName) {
        ChunkConfig config = getRecommendedConfig(text);
        // 最小块大小设为最大块的30%
        int minChunkSize = (int)(config.getChunkSize() * 0.3);
        return chunkToDocuments(text, fileName, config.getChunkSize(), minChunkSize, config.getChunkOverlap());
    }
    
    /**
     * 规范化文本
     */
    private static String normalizeText(String text) {
        text = text.replace("\r\n", "\n").replace("\r", "\n");
        
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
     * 优先级：【】标题 > 中文章节标题 > 段落边界 > 句子边界 > 换行 > 空格
     */
    private static int findBestSplitPoint(String text, int start, int end, int chunkSize) {
        int searchStart = Math.max(start, end - Math.min(100, chunkSize / 3));
        int searchEnd = Math.min(text.length(), end + Math.min(50, chunkSize / 6));
        
        // 优先：【】标题边界
        int bracketPos = findLastMatch(text, BRACKET_HEADING_PATTERN, searchStart, searchEnd);
        if (bracketPos > searchStart) {
            return bracketPos;
        }
        
        // 次优：中文章节标题边界
        int chineseHeadingPos = findLastMatch(text, CHINESE_HEADING_PATTERN, searchStart, searchEnd);
        if (chineseHeadingPos > searchStart) {
            return chineseHeadingPos;
        }
        
        // 段落边界
        int paragraphEnd = text.lastIndexOf(PARAGRAPH_SEPARATOR, searchEnd);
        if (paragraphEnd > searchStart) {
            return paragraphEnd + PARAGRAPH_SEPARATOR.length();
        }
        
        // 句子边界
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
        
        // 换行边界
        int newlinePos = text.lastIndexOf('\n', end);
        if (newlinePos > searchStart) {
            return newlinePos + 1;
        }
        
        // 空格边界
        int spacePos = text.lastIndexOf(' ', end);
        if (spacePos > searchStart) {
            return spacePos + 1;
        }
        
        return end;
    }
    
    /**
     * 在指定范围内查找最后一个匹配模式的位置
     */
    private static int findLastMatch(String text, Pattern pattern, int searchStart, int searchEnd) {
        String searchArea = text.substring(0, Math.min(searchEnd, text.length()));
        Matcher matcher = pattern.matcher(searchArea);
        int lastPos = -1;
        while (matcher.find()) {
            if (matcher.start() >= searchStart && matcher.start() < searchEnd) {
                lastPos = matcher.start();
            }
        }
        return lastPos;
    }
    
    /**
     * 计算文本的MD5哈希值
     */
    public static String calculateMD5(String text) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(text.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (java.security.NoSuchAlgorithmException e) {
            logger.error("MD5算法不可用", e);
            throw new RuntimeException("MD5计算失败", e);
        }
    }
}
