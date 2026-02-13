package com.redmoon2333.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.redmoon2333.config.RagConfig;
import com.redmoon2333.config.VectorStoreConfig;
import com.redmoon2333.dto.RagInitRequest;
import com.redmoon2333.dto.RagInitResponse;
import com.redmoon2333.dto.RagStatsResponse;
import com.redmoon2333.util.DocumentParser;
import com.redmoon2333.util.MemoryMonitor;
import com.redmoon2333.util.SmartTextChunker;

import cn.hutool.crypto.SecureUtil;
import redis.clients.jedis.JedisPooled;

/**
 * RAG管理服务
 * 使用 Spring AI Redis VectorStore 进行知识库管理
 * 
 * 迁移说明：
 * - 从 Qdrant 迁移到 Redis Stack 向量存储
 * - MD5去重缓存改用 Redis Set 存储
 * - 统计信息通过 Redis 命令获取
 * 
 * Warning: 需要 Redis 服务端支持 RediSearch 和 RedisJSON 模块
 */
@Service
public class RagManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagManagementService.class);
    
    private static final String MD5_CACHE_KEY = "rag:md5_cache";
    private static final String STATS_KEY = "rag:stats";
    
    @Autowired
    private VectorStore vectorStore;
    
    @Autowired
    private RagConfig ragConfig;
    
    @Autowired
    private VectorStoreConfig vectorStoreConfig;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    @Autowired
    private JedisPooled jedisPooled;
    
    /**
     * 初始化向量数据库
     * 使用 Spring AI VectorStore，自动处理 embedding 和存储
     * 
     * Warning: 添加文档路径存在性校验，避免初始化成功但无数据的误导
     * 
     * 内存优化说明（v2.0）：
     * - 添加内存监控和自动GC机制，防止低配服务器OOM
     * - 支持低内存模式，适用于2核2G等低配服务器
     * - 文件大小限制，跳过超大文件
     * - 处理间隔控制，降低CPU和内存峰值
     * 
     * 分块优化说明（v2.1）：
     * - 使用SmartTextChunker进行语义分块
     * - 按章节、段落、句子边界智能分割
     * - 自动识别文档类型，选择最佳分块策略
     * - 小片段智能合并，避免过度碎片化
     */
    public RagInitResponse initializeKnowledgeBase(RagInitRequest request) {
        logger.info("开始初始化知识库，源路径: {}, 强制重建: {}, 低内存模式: {}", 
                   request.getSourcePath(), request.getForceReindex(), ragConfig.isLowMemoryMode());
        
        // 记录初始内存状态
        MemoryMonitor.logMemoryStatus();
        
        RagInitResponse response = new RagInitResponse();
        
        try {
            String sourcePath = request.getSourcePath() != null ? 
                              request.getSourcePath() : ragConfig.getKnowledgeBasePath();
            
            // 防御性编程：检查文档路径是否存在
            Path baseDir = Paths.get(sourcePath);
            if (!Files.exists(baseDir)) {
                logger.error("知识库路径不存在: {}", sourcePath);
                throw new RuntimeException("知识库路径不存在: " + sourcePath + "，请检查配置");
            }
            
            if (!Files.isDirectory(baseDir)) {
                logger.error("知识库路径不是目录: {}", sourcePath);
                throw new RuntimeException("知识库路径必须是目录: " + sourcePath);
            }
            
            List<Path> files = scanFiles(sourcePath);
            response.setTotalFiles(files.size());
            
            // 警告：如果未找到任何文件，记录警告日志
            if (files.isEmpty()) {
                logger.warn("在路径 {} 下未找到任何支持的文档文件", sourcePath);
            }
            
            logger.info("共扫描到 {} 个文件", files.size());
            
            Set<String> existingMd5s = request.getForceReindex() ? 
                new HashSet<>() : buildMd5Cache();
            logger.info("已加载 {} 个已存在的文件MD5", existingMd5s.size());
            
            int processedCount = 0;
            int failedCount = 0;
            int skippedCount = 0;
            int totalChunks = 0;
            int newChunks = 0;
            int duplicateChunks = 0;
            
            // 获取配置参数
            int effectiveBatchSize = ragConfig.getEffectiveBatchSize();
            long fileDelayMs = ragConfig.getEffectiveFileDelayMs();
            long maxFileSizeBytes = ragConfig.getMaxFileSizeMB() * 1024 * 1024;
            
            logger.info("处理参数 - 批大小: {}, 文件间隔: {}ms, 最大文件: {}MB",
                effectiveBatchSize, fileDelayMs, ragConfig.getMaxFileSizeMB());
            
            for (Path file : files) {
                try {
                    // ========== 内存保护：处理前检查 ==========
                    if (checkMemoryAndWaitIfNeeded()) {
                        logger.warn("内存持续紧张，跳过文件: {}", file.getFileName());
                        skippedCount++;
                        response.getErrors().add(new RagInitResponse.FileError(
                            file.getFileName().toString(),
                            "内存不足，跳过处理"
                        ));
                        continue;
                    }
                    
                    // ========== 文件大小检查 ==========
                    if (maxFileSizeBytes > 0) {
                        long fileSize = Files.size(file);
                        if (fileSize > maxFileSizeBytes) {
                            logger.warn("文件 {} 大小 {}MB 超过限制 {}MB，跳过",
                                file.getFileName(), fileSize / 1024 / 1024, ragConfig.getMaxFileSizeMB());
                            skippedCount++;
                            response.getErrors().add(new RagInitResponse.FileError(
                                file.getFileName().toString(),
                                "文件过大(" + (fileSize / 1024 / 1024) + "MB)，跳过处理"
                            ));
                            continue;
                        }
                    }
                    
                    logger.info("处理文件: {}", file.getFileName());
                    
                    // 读取文件内容
                    String fileContent = DocumentParser.parseDocument(file);
                    String fileMd5 = SecureUtil.md5(fileContent);
                    
                    if (existingMd5s.contains(fileMd5)) {
                        logger.info("文件 {} 已存在，跳过", file.getFileName());
                        duplicateChunks++;
                        // 尽早释放fileContent引用
                        fileContent = null;
                        continue;
                    }
                    
                    // ========== 使用智能分块器 ==========
                    List<Document> splitDocuments;
                    if (ragConfig.isEnableSemanticChunking()) {
                        // 语义分块：按章节、段落、句子边界智能分割
                        splitDocuments = SmartTextChunker.chunkToDocuments(
                            fileContent, 
                            file.getFileName().toString(),
                            ragConfig.getChunkSize(),
                            ragConfig.getMinChunkSize(),
                            ragConfig.getChunkOverlap()
                        );
                        logger.info("使用语义分块，文件: {}，分块数: {}", 
                            file.getFileName(), splitDocuments.size());
                    } else {
                        // 基础分块：按固定大小分割
                        List<String> chunks = SmartTextChunker.chunkText(
                            fileContent, 
                            ragConfig.getChunkSize(), 
                            ragConfig.getChunkOverlap()
                        );
                        splitDocuments = new ArrayList<>();
                        for (int idx = 0; idx < chunks.size(); idx++) {
                            Map<String, Object> metadata = new HashMap<>();
                            metadata.put("source", file.getFileName().toString());
                            metadata.put("chunk_index", idx);
                            splitDocuments.add(new Document(chunks.get(idx), metadata));
                        }
                        // 立即释放chunks
                        chunks.clear();
                    }
                    
                    // 尽早释放fileContent引用
                    fileContent = null;
                    
                    totalChunks += splitDocuments.size();
                    
                    // 为每个文档添加通用元数据
                    final String finalFileMd5 = fileMd5;
                    final String fileName = file.getFileName().toString();
                    final String createdAt = Instant.now().toString();
                    splitDocuments.forEach(doc -> {
                        doc.getMetadata().put("file_md5", finalFileMd5);
                        doc.getMetadata().put("file_name", fileName);
                        doc.getMetadata().put("created_at", createdAt);
                    });
                    
                    // ========== 分批处理，添加内存检查 ==========
                    for (int i = 0; i < splitDocuments.size(); i += effectiveBatchSize) {
                        // 每批处理前检查内存
                        checkMemoryAndWaitIfNeeded();
                        
                        int endIndex = Math.min(i + effectiveBatchSize, splitDocuments.size());
                        List<Document> batch = new ArrayList<>(splitDocuments.subList(i, endIndex));
                        
                        try {
                            vectorStore.add(batch);
                            newChunks += batch.size();
                        } finally {
                            // 确保批次列表被清理
                            batch.clear();
                        }
                        
                        // 低内存模式下，每批处理后主动GC
                        if (ragConfig.isLowMemoryMode()) {
                            MemoryMonitor.checkAndReleaseIfNeeded(ragConfig.getMemoryWarningThreshold());
                        }
                    }
                    
                    existingMd5s.add(fileMd5);
                    stringRedisTemplate.opsForSet().add(MD5_CACHE_KEY, fileMd5);
                    
                    processedCount++;
                    logger.info("文件处理成功: {}，分块数: {}", file.getFileName(), splitDocuments.size());
                    
                    // 清理分块文档列表
                    splitDocuments.clear();
                    splitDocuments = null;
                    
                    // ========== 文件处理间隔 ==========
                    if (fileDelayMs > 0) {
                        Thread.sleep(fileDelayMs);
                    }
                    
                    // ========== 每个文件处理后主动GC（低内存模式） ==========
                    if (ragConfig.isLowMemoryMode()) {
                        MemoryMonitor.tryReleaseMemory();
                    }
                    
                } catch (OutOfMemoryError oom) {
                    logger.error("处理文件 {} 时内存不足", file.getFileName(), oom);
                    // 紧急GC
                    MemoryMonitor.tryReleaseMemory();
                    failedCount++;
                    response.getErrors().add(new RagInitResponse.FileError(
                        file.getFileName().toString(),
                        "内存不足: " + oom.getMessage()
                    ));
                    
                    // OOM后等待更长时间让系统恢复
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } catch (InterruptedException ie) {
                    logger.warn("处理被中断");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    failedCount++;
                    logger.error("文件处理失败: {}", file.getFileName(), e);
                    response.getErrors().add(new RagInitResponse.FileError(
                        file.getFileName().toString(),
                        e.getMessage()
                    ));
                }
            }
            
            response.setProcessedFiles(processedCount);
            response.setFailedFiles(failedCount);
            response.setTotalChunks(totalChunks);
            response.setNewChunks(newChunks);
            response.setDuplicateChunks(duplicateChunks);
            
            updateStats(response);
            
            // 最终内存状态
            MemoryMonitor.logMemoryStatus();
            
            logger.info("知识库初始化完成，成功: {}, 失败: {}, 跳过: {}, 新增向量: {}, 重复: {}", 
                       processedCount, failedCount, skippedCount, newChunks, duplicateChunks);
            
        } catch (Exception e) {
            logger.error("知识库初始化失败", e);
            throw new RuntimeException("知识库初始化失败: " + e.getMessage(), e);
        }
        
        return response;
    }
    
    /**
     * 检查内存状态，必要时等待内存释放
     * 
     * @return true表示内存持续紧张（即使等待后仍然紧张），应跳过当前处理
     */
    private boolean checkMemoryAndWaitIfNeeded() {
        double warningThreshold = ragConfig.getMemoryWarningThreshold();
        double criticalThreshold = ragConfig.getMemoryCriticalThreshold();
        
        // 检查是否达到警告阈值
        if (MemoryMonitor.isMemoryPressure(warningThreshold)) {
            logger.warn("内存使用率达到警告阈值，尝试GC释放");
            MemoryMonitor.tryReleaseMemory();
        }
        
        // 检查是否达到危险阈值
        if (MemoryMonitor.isMemoryPressure(criticalThreshold)) {
            logger.error("内存使用率达到危险阈值 {}%，等待内存释放...", (int)(criticalThreshold * 100));
            
            // 等待最多10秒让内存降下来
            boolean released = MemoryMonitor.waitForMemoryRelease(criticalThreshold, 10000, 500);
            
            if (!released) {
                logger.error("内存无法释放到安全水平，当前使用率: {}%", 
                    (int)(MemoryMonitor.getMemoryUsageRatio() * 100));
                return true; // 表示应该跳过当前处理
            }
        }
        
        return false;
    }
    
    /**
     * 获取知识库统计信息
     * 
     * @return 统计信息
     */
    public RagStatsResponse getStats() {
        try {
            logger.info("获取知识库统计信息");

            RagStatsResponse stats = new RagStatsResponse();
            stats.setCollectionName(vectorStoreConfig.getIndexName());

            // 获取向量数量 - 优先使用 FT.INFO 命令获取准确的索引文档数
            long vectorCount = 0L;
            try {
                // 使用 Jedis 执行 FT.INFO 命令获取索引信息
                Map<String, Object> indexInfo = jedisPooled.ftInfo(vectorStoreConfig.getIndexName());
                // FT.INFO 返回的 num_docs 字段表示索引中的文档数量
                Object numDocs = indexInfo.get("num_docs");
                if (numDocs != null) {
                    vectorCount = Long.parseLong(numDocs.toString());
                    logger.info("从 FT.INFO 获取到向量数量: {}", vectorCount);
                }
            } catch (Exception e) {
                logger.warn("无法从 FT.INFO 获取向量数量，降级使用 Redis 缓存: {}", e.getMessage());
                // 降级：使用 Redis 缓存的累加值
                String cachedCount = stringRedisTemplate.opsForValue().get(STATS_KEY + ":total_vectors");
                vectorCount = cachedCount != null ? Long.parseLong(cachedCount) : 0L;
            }
            stats.setTotalVectors(vectorCount);

            // 获取文档数量（从MD5缓存中统计）
            Set<String> fileMd5s = stringRedisTemplate.opsForSet().members(MD5_CACHE_KEY);
            int documentCount = (fileMd5s != null) ? fileMd5s.size() : 0;
            stats.setTotalDocuments(documentCount);

            stats.setVectorDimension(ragConfig.getVectorDimension());

            // 获取最后更新时间
            String lastUpdate = stringRedisTemplate.opsForValue().get(STATS_KEY + ":last_update");
            stats.setLastUpdateTime(lastUpdate != null ? lastUpdate : Instant.now().toString());

            stats.setCategoryStats(new HashMap<>());

            logger.info("统计信息获取成功，文档总数: {}, 向量总数: {}", 
                stats.getTotalDocuments(), stats.getTotalVectors());
            return stats;

        } catch (Exception e) {
            logger.error("获取统计信息失败", e);
            throw new RuntimeException("获取统计信息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 扫描文件目录
     * 
     * @param basePath 基础路径
     * @return 文件列表
     */
    private List<Path> scanFiles(String basePath) throws Exception {
        List<Path> files = new ArrayList<>();
        Path baseDir = Paths.get(basePath);
        
        if (!Files.exists(baseDir)) {
            logger.warn("目录不存在: {}", basePath);
            return files;
        }
        
        Files.walk(baseDir)
            .filter(Files::isRegularFile)
            .filter(path -> DocumentParser.isSupportedFile(path.getFileName().toString()))
            .forEach(files::add);
        
        return files;
    }
    
    /**
     * 构建已存在文件的MD5缓存
     * 使用 Redis Set 存储已处理的文件MD5
     */
    private Set<String> buildMd5Cache() {
        Set<String> md5Set = new HashSet<>();
        try {
            logger.debug("开始构建MD5缓存...");
            
            Set<String> redisSet = stringRedisTemplate.opsForSet().members(MD5_CACHE_KEY);
            if (redisSet != null) {
                md5Set.addAll(redisSet);
            }
            
            logger.debug("MD5缓存构建完成，共加载 {} 个MD5", md5Set.size());
            
        } catch (Exception e) {
            logger.warn("构建MD5缓存失败，将跳过去重检查: {}", e.getMessage());
        }
        
        return md5Set;
    }
    
    /**
     * 更新统计信息到Redis
     * Warning: 使用累加方式保存向量总数，确保统计准确性
     */
    private void updateStats(RagInitResponse response) {
        try {
            // 累加向量数（而非覆盖）
            String currentCountStr = stringRedisTemplate.opsForValue().get(STATS_KEY + ":total_vectors");
            long currentCount = currentCountStr != null ? Long.parseLong(currentCountStr) : 0L;
            long newTotal = currentCount + response.getNewChunks();

            stringRedisTemplate.opsForValue().set(STATS_KEY + ":total_vectors",
                String.valueOf(newTotal));
            stringRedisTemplate.opsForValue().set(STATS_KEY + ":last_update",
                Instant.now().toString());

            logger.info("统计信息已更新，新增向量: {}, 累计向量: {}",
                response.getNewChunks(), newTotal);
        } catch (Exception e) {
            logger.warn("更新统计信息失败: {}", e.getMessage());
        }
    }
    
    /**
     * 清空知识库
     * 删除Redis中的所有向量数据和MD5缓存
     */
    public void clearKnowledgeBase() {
        try {
            logger.warn("正在清空知识库...");
            
            stringRedisTemplate.delete(MD5_CACHE_KEY);
            stringRedisTemplate.delete(stringRedisTemplate.keys(STATS_KEY + "*"));
            
            String prefix = vectorStoreConfig.getPrefix();
            Set<String> keys = stringRedisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
            
            logger.info("知识库已清空");
        } catch (Exception e) {
            logger.error("清空知识库失败", e);
            throw new RuntimeException("清空知识库失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 列出所有已存储的文件
     * 从Redis MD5缓存中获取文件信息
     * 
     * @return 文件MD5列表
     */
    public Set<String> listAllFiles() {
        Set<String> fileMd5s = new HashSet<>();
        try {
            logger.info("开始获取所有文件MD5列表...");
            
            Set<String> redisSet = stringRedisTemplate.opsForSet().members(MD5_CACHE_KEY);
            if (redisSet != null) {
                fileMd5s.addAll(redisSet);
            }
            
            logger.info("获取文件MD5列表完成，共 {} 个文件", fileMd5s.size());
            
        } catch (Exception e) {
            logger.error("获取文件列表失败", e);
        }
        
        return fileMd5s;
    }
}
