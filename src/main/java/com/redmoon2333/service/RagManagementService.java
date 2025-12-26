package com.redmoon2333.service;

import com.redmoon2333.config.QdrantConfig;
import com.redmoon2333.config.RagConfig;
import com.redmoon2333.dto.RagInitRequest;
import com.redmoon2333.dto.RagInitResponse;
import com.redmoon2333.dto.RagStatsResponse;
import com.redmoon2333.util.DocumentParser;
import com.redmoon2333.util.TextChunker;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.*;
import io.qdrant.client.grpc.Points.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * RAG管理服务
 * 负责知识库的初始化、同步和管理
 */
@Service
public class RagManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagManagementService.class);
    
    @Autowired
    private QdrantClient qdrantClient;
    
    @Autowired
    private QdrantConfig qdrantConfig;
    
    @Autowired
    private RagConfig ragConfig;
    
    @Autowired
    private EmbeddingService embeddingService;
    
    /**
     * 初始化向量数据库
     * 
     * @param request 初始化请求
     * @return 初始化结果
     */
    public RagInitResponse initializeKnowledgeBase(RagInitRequest request) {
        logger.info("开始初始化知识库，源路径: {}, 强制重建: {}", 
                   request.getSourcePath(), request.getForceReindex());
        
        RagInitResponse response = new RagInitResponse();
        
        try {
            // 1. 确保Collection存在
            ensureCollectionExists(request.getForceReindex());
            
            // 2. 扫描文件
            String sourcePath = request.getSourcePath() != null ? 
                              request.getSourcePath() : ragConfig.getKnowledgeBasePath();
            List<Path> files = scanFiles(sourcePath);
            response.setTotalFiles(files.size());
            
            logger.info("共扫描到 {} 个文件", files.size());
            
            // 3. 处理每个文件
            int processedCount = 0;
            int failedCount = 0;
            int totalChunks = 0;
            int newChunks = 0;
            int duplicateChunks = 0;
            
            for (Path file : files) {
                try {
                    logger.info("处理文件: {}", file.getFileName());
                    
                    // 解析文档
                    String content = DocumentParser.parseDocument(file);
                    
                    // 分块
                    List<String> chunks = TextChunker.chunkText(
                        content, 
                        ragConfig.getChunkSize(), 
                        ragConfig.getChunkOverlap()
                    );
                    
                    totalChunks += chunks.size();
                    
                    // 处理每个分块
                    for (int i = 0; i < chunks.size(); i++) {
                        String chunk = chunks.get(i);
                        String md5 = TextChunker.calculateMD5(chunk);
                        
                        // 检查是否已存在
                        if (isChunkExists(md5)) {
                            duplicateChunks++;
                            logger.debug("跳过重复分块: {}", md5);
                            continue;
                        }
                        
                        // 向量化并存储
                        float[] vector = embeddingService.embedText(chunk);
                        storeVector(file.getFileName().toString(), chunk, vector, md5, i);
                        newChunks++;
                    }
                    
                    processedCount++;
                    logger.info("文件处理成功: {}，分块数: {}", file.getFileName(), chunks.size());
                    
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
            
            logger.info("知识库初始化完成，成功: {}, 失败: {}, 新增向量: {}, 重复: {}", 
                       processedCount, failedCount, newChunks, duplicateChunks);
            
        } catch (Exception e) {
            logger.error("知识库初始化失败", e);
            throw new RuntimeException("知识库初始化失败: " + e.getMessage(), e);
        }
        
        return response;
    }
    
    /**
     * 获取知识库统计信息
     * 
     * @return 统计信息
     */
    public RagStatsResponse getStats() {
        try {
            logger.info("获取知识库统计信息");
            
            // 获取Collection信息
            String collectionName = qdrantConfig.getCollectionName();
            CollectionInfo collectionInfo = qdrantClient.getCollectionInfoAsync(collectionName)
                .get();
            
            RagStatsResponse stats = new RagStatsResponse();
            stats.setCollectionName(collectionName);
            stats.setTotalVectors(collectionInfo.getPointsCount());
            stats.setVectorDimension(ragConfig.getVectorDimension());
            stats.setLastUpdateTime(Instant.now().toString());
            
            // TODO: 实现分类统计（需要遍历所有点或维护单独的统计）
            stats.setCategoryStats(new HashMap<>());
            
            logger.info("统计信息获取成功，向量总数: {}", stats.getTotalVectors());
            return stats;
            
        } catch (Exception e) {
            logger.error("获取统计信息失败", e);
            throw new RuntimeException("获取统计信息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 确保Collection存在
     * 
     * @param forceRecreate 是否强制重建
     */
    private void ensureCollectionExists(boolean forceRecreate) 
            throws ExecutionException, InterruptedException {
        String collectionName = qdrantConfig.getCollectionName();
        
        // 检查Collection是否存在
        boolean exists = qdrantClient.collectionExistsAsync(collectionName).get();
        
        if (exists && forceRecreate) {
            logger.info("删除已存在的Collection: {}", collectionName);
            qdrantClient.deleteCollectionAsync(collectionName).get();
            exists = false;
        }
        
        if (!exists) {
            logger.info("创建新Collection: {}", collectionName);
            
            VectorParams vectorParams = VectorParams.newBuilder()
                .setSize(ragConfig.getVectorDimension())
                .setDistance(Distance.Cosine)
                .build();
            
            CreateCollection createCollection = CreateCollection.newBuilder()
                .setCollectionName(collectionName)
                .setVectorsConfig(VectorsConfig.newBuilder()
                    .setParams(vectorParams)
                    .build())
                .build();
            
            qdrantClient.createCollectionAsync(createCollection).get();
            logger.info("Collection创建成功");
        } else {
            logger.info("Collection已存在: {}", collectionName);
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
     * 检查分块是否已存在
     * 
     * @param md5 MD5哈希值
     * @return 是否存在
     */
    private boolean isChunkExists(String md5) {
        try {
            // 使用scroll方法搜索
            Filter filter = Filter.newBuilder()
                .addMust(Condition.newBuilder()
                    .setField(FieldCondition.newBuilder()
                        .setKey("content_md5")
                        .setMatch(Match.newBuilder()
                            .setKeyword(md5)
                            .build())
                        .build())
                    .build())
                .build();
            
            ScrollPoints scrollPoints = ScrollPoints.newBuilder()
                .setCollectionName(qdrantConfig.getCollectionName())
                .setFilter(filter)
                .setLimit(1)
                .build();
            
            List<ScoredPoint> results = qdrantClient.scrollAsync(scrollPoints).get();
            return !results.isEmpty();
            
        } catch (Exception e) {
            logger.error("检查分块是否存在时出错", e);
            return false;
        }
    }
    
    /**
     * 存储向量到Qdrant
     * 
     * @param fileName 文件名
     * @param content 内容
     * @param vector 向量
     * @param md5 MD5哈希值
     * @param chunkIndex 分块索引
     */
    private void storeVector(String fileName, String content, float[] vector, 
                           String md5, int chunkIndex) throws ExecutionException, InterruptedException {
        
        // 生成唯一ID
        String pointId = UUID.randomUUID().toString();
        
        // 构建metadata
        Map<String, Value> payload = new HashMap<>();
        payload.put("content", Value.newBuilder().setStringValue(content).build());
        payload.put("file_name", Value.newBuilder().setStringValue(fileName).build());
        payload.put("content_md5", Value.newBuilder().setStringValue(md5).build());
        payload.put("chunk_index", Value.newBuilder().setIntegerValue(chunkIndex).build());
        payload.put("created_at", Value.newBuilder().setStringValue(Instant.now().toString()).build());
        
        // 转换向量格式
        List<Float> vectorList = new ArrayList<>();
        for (float v : vector) {
            vectorList.add(v);
        }
        
        // 构建Point
        PointStruct point = PointStruct.newBuilder()
            .setId(PointId.newBuilder().setUuid(pointId).build())
            .setVectors(Vectors.newBuilder().setVector(
                io.qdrant.client.grpc.Points.Vector.newBuilder()
                    .addAllData(vectorList)
                    .build())
                .build())
            .putAllPayload(payload)
            .build();
        
        // 存储到Qdrant
        UpsertPoints upsertPoints = UpsertPoints.newBuilder()
            .setCollectionName(qdrantConfig.getCollectionName())
            .addPoints(point)
            .build();
        
        qdrantClient.upsertAsync(upsertPoints).get();
        
        logger.trace("向量存储成功: {} - 分块 {}", fileName, chunkIndex);
    }
}
