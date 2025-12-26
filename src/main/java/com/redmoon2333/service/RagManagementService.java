package com.redmoon2333.service;

import cn.hutool.crypto.SecureUtil;
import com.redmoon2333.config.QdrantConfig;
import com.redmoon2333.config.RagConfig;
import com.redmoon2333.dto.RagInitRequest;
import com.redmoon2333.dto.RagInitResponse;
import com.redmoon2333.dto.RagStatsResponse;
import com.redmoon2333.util.DocumentParser;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.*;
import io.qdrant.client.grpc.Points.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * RAG管理服务
 * 使用 Spring AI VectorStore 进行知识库管理
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
    private VectorStore vectorStore;
    
    /**
     * 初始化向量数据库
     * 使用 Spring AI VectorStore，自动处理 embedding 和存储
     */
    public RagInitResponse initializeKnowledgeBase(RagInitRequest request) {
        logger.info("开始初始化知识库，源路径: {}, 强制重建: {}", 
                   request.getSourcePath(), request.getForceReindex());
        
        RagInitResponse response = new RagInitResponse();
        
        try {
            // 1. 如果强制重建，删除旧collection
            if (request.getForceReindex()) {
                deleteCollection();
            }
            
            // 2. 扫描文件
            String sourcePath = request.getSourcePath() != null ? 
                              request.getSourcePath() : ragConfig.getKnowledgeBasePath();
            List<Path> files = scanFiles(sourcePath);
            response.setTotalFiles(files.size());
            
            logger.info("共扫描到 {} 个文件", files.size());
            
            // 3. 构建MD5缓存（用于去重）
            Set<String> existingMd5s = buildMd5Cache();
            logger.info("已加载 {} 个已存在的文件MD5", existingMd5s.size());
            
            // 4. 处理每个文件
            int processedCount = 0;
            int failedCount = 0;
            int totalChunks = 0;
            int newChunks = 0;
            int duplicateChunks = 0;
            
            for (Path file : files) {
                try {
                    logger.info("处理文件: {}", file.getFileName());
                    
                    // 计算文件内容的MD5
                    String fileContent = DocumentParser.parseDocument(file);
                    String fileMd5 = SecureUtil.md5(fileContent);
                    
                    // 检查是否已存在
                    if (existingMd5s.contains(fileMd5)) {
                        logger.info("文件 {} 已存在，跳过", file.getFileName());
                        duplicateChunks++;
                        continue;
                    }
                    
                    // 使用Spring AI的TextReader读取文件
                    TextReader textReader = new TextReader(new FileSystemResource(file.toFile()));
                    textReader.setCharset(StandardCharsets.UTF_8);
                    List<Document> documents = textReader.read();
                    
                    // 使用TokenTextSplitter分割文本
                    TokenTextSplitter splitter = new TokenTextSplitter(
                        ragConfig.getChunkSize(),
                        ragConfig.getChunkOverlap(),
                        5,      // minChunkLengthToEmbed
                        10000,  // maxNumChunks
                        true    // keepSeparator
                    );
                    List<Document> splitDocuments = splitter.transform(documents);
                    
                    totalChunks += splitDocuments.size();
                    
                    // 为每个文档添加元数据（用于去重）
                    splitDocuments.forEach(doc -> {
                        doc.getMetadata().put("file_md5", fileMd5);
                        doc.getMetadata().put("file_name", file.getFileName().toString());
                        doc.getMetadata().put("created_at", Instant.now().toString());
                    });
                    
                    // 使用VectorStore批量添加（自动处理embedding和存储）
                    vectorStore.add(splitDocuments);
                    newChunks += splitDocuments.size();
                    
                    // 添加到缓存
                    existingMd5s.add(fileMd5);
                    
                    processedCount++;
                    logger.info("文件处理成功: {}，分块数: {}", file.getFileName(), splitDocuments.size());
                    
                    // 释放内存引用（这些是不可变集合，无需clear，GC会自动回收）
                    documents = null;
                    splitDocuments = null;
                    
                } catch (OutOfMemoryError oom) {
                    logger.error("处理文件 {} 时内存不足", file.getFileName(), oom);
                    System.gc();
                    failedCount++;
                    response.getErrors().add(new RagInitResponse.FileError(
                        file.getFileName().toString(),
                        "内存不足: " + oom.getMessage()
                    ));
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
     * 构建已存在文件的MD5缓存
     */
    private Set<String> buildMd5Cache() {
        Set<String> md5Set = new HashSet<>();
        try {
            logger.debug("开始构建MD5缓存...");
            
            ScrollPoints scrollPoints = ScrollPoints.newBuilder()
                .setCollectionName(qdrantConfig.getCollectionName())
                .setLimit(100)
                .setWithPayload(WithPayloadSelector.newBuilder()
                    .setInclude(PayloadIncludeSelector.newBuilder()
                        .addFields("file_md5")
                        .build())
                    .build())
                .build();
            
            ScrollResponse response = qdrantClient.scrollAsync(scrollPoints).get();
            
            for (RetrievedPoint point : response.getResultList()) {
                if (point.getPayloadMap().containsKey("file_md5")) {
                    String md5 = point.getPayloadMap().get("file_md5").getStringValue();
                    md5Set.add(md5);
                }
            }
            
            logger.debug("MD5缓存构建完成，共加载 {} 个MD5", md5Set.size());
        } catch (Exception e) {
            logger.warn("构建MD5缓存失败，将跳过去重检查: {}", e.getMessage());
        }
        
        return md5Set;
    }
    
    /**
     * 删除Collection
     */
    private void deleteCollection() {
        try {
            String collectionName = qdrantConfig.getCollectionName();
            logger.warn("正在删除Collection: {}", collectionName);
            qdrantClient.deleteCollectionAsync(collectionName).get();
            logger.info("成功删除Collection: {}", collectionName);
            Thread.sleep(1000); // 等待删除完成
        } catch (Exception e) {
            logger.warn("删除Collection失败（可能不存在）: {}", e.getMessage());
        }
    }
    
}
