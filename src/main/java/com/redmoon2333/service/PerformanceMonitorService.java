package com.redmoon2333.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 性能监控服务
 * 收集和统计系统性能指标，用于优化效果评估
 * 
 * Why: 性能优化需要数据支撑，通过监控可以量化优化效果
 * 
 * Warning: 监控本身会带来少量性能开销，生产环境可关闭详细监控
 */
@Service
public class PerformanceMonitorService {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitorService.class);
    
    // 检索性能指标
    private final AtomicLong totalRetrievalCount = new AtomicLong(0);
    private final AtomicLong cacheHitCount = new AtomicLong(0);
    private final AtomicLong totalRetrievalTime = new AtomicLong(0);
    private final AtomicReference<Double> avgRetrievalTime = new AtomicReference<>(0.0);
    
    // 意图识别性能指标
    private final AtomicLong totalIntentRecognitionCount = new AtomicLong(0);
    private final AtomicLong ruleBasedCount = new AtomicLong(0);
    private final AtomicLong llmBasedCount = new AtomicLong(0);
    private final AtomicLong llmCacheHitCount = new AtomicLong(0);
    private final AtomicLong totalIntentTime = new AtomicLong(0);
    
    // 对话性能指标
    private final AtomicLong totalChatCount = new AtomicLong(0);
    private final AtomicLong totalChatTime = new AtomicLong(0);
    private final Map<String, AtomicLong> userChatCounts = new ConcurrentHashMap<>();
    
    /**
     * 记录检索操作
     * 
     * @param duration 耗时（毫秒）
     * @param cacheHit 是否命中缓存
     */
    public void recordRetrieval(long duration, boolean cacheHit) {
        totalRetrievalCount.incrementAndGet();
        totalRetrievalTime.addAndGet(duration);
        
        if (cacheHit) {
            cacheHitCount.incrementAndGet();
        }
        
        // 更新平均耗时
        long count = totalRetrievalCount.get();
        double newAvg = (double) totalRetrievalTime.get() / count;
        avgRetrievalTime.set(newAvg);
        
        logger.debug("检索耗时: {}ms, 缓存命中: {}", duration, cacheHit);
    }
    
    /**
     * 记录意图识别操作
     * 
     * @param duration 耗时（毫秒）
     * @param method 识别方法（rule/llm/llm-cache）
     */
    public void recordIntentRecognition(long duration, String method) {
        totalIntentRecognitionCount.incrementAndGet();
        totalIntentTime.addAndGet(duration);
        
        switch (method) {
            case "rule":
                ruleBasedCount.incrementAndGet();
                break;
            case "llm":
                llmBasedCount.incrementAndGet();
                break;
            case "llm-cache":
                llmCacheHitCount.incrementAndGet();
                break;
        }
        
        logger.debug("意图识别耗时: {}ms, 方法: {}", duration, method);
    }
    
    /**
     * 记录对话操作
     * 
     * @param userId 用户ID
     * @param duration 耗时（毫秒）
     */
    public void recordChat(String userId, long duration) {
        totalChatCount.incrementAndGet();
        totalChatTime.addAndGet(duration);
        
        userChatCounts.computeIfAbsent(userId, k -> new AtomicLong(0)).incrementAndGet();
        
        logger.debug("用户 {} 对话耗时: {}ms", userId, duration);
    }
    
    /**
     * 获取性能统计报告
     */
    public PerformanceReport getReport() {
        PerformanceReport report = new PerformanceReport();
        
        // 检索统计
        report.setTotalRetrievals(totalRetrievalCount.get());
        report.setCacheHits(cacheHitCount.get());
        report.setCacheHitRate(calculateRate(cacheHitCount.get(), totalRetrievalCount.get()));
        report.setAvgRetrievalTime(avgRetrievalTime.get());
        
        // 意图识别统计
        report.setTotalIntentRecognitions(totalIntentRecognitionCount.get());
        report.setRuleBasedRate(calculateRate(ruleBasedCount.get(), totalIntentRecognitionCount.get()));
        report.setLlmBasedRate(calculateRate(llmBasedCount.get(), totalIntentRecognitionCount.get()));
        report.setLlmCacheHitRate(calculateRate(llmCacheHitCount.get(), llmBasedCount.get() + llmCacheHitCount.get()));
        report.setAvgIntentTime((double) totalIntentTime.get() / Math.max(totalIntentRecognitionCount.get(), 1));
        
        // 对话统计
        report.setTotalChats(totalChatCount.get());
        report.setAvgChatTime((double) totalChatTime.get() / Math.max(totalChatCount.get(), 1));
        report.setActiveUsers(userChatCounts.size());
        
        return report;
    }
    
    private double calculateRate(long part, long total) {
        if (total == 0) return 0.0;
        return Math.round((double) part / total * 10000) / 100.0; // 保留两位小数
    }
    
    /**
     * 重置统计数据
     */
    public void reset() {
        totalRetrievalCount.set(0);
        cacheHitCount.set(0);
        totalRetrievalTime.set(0);
        avgRetrievalTime.set(0.0);
        
        totalIntentRecognitionCount.set(0);
        ruleBasedCount.set(0);
        llmBasedCount.set(0);
        llmCacheHitCount.set(0);
        totalIntentTime.set(0);
        
        totalChatCount.set(0);
        totalChatTime.set(0);
        userChatCounts.clear();
        
        logger.info("性能监控数据已重置");
    }
    
    /**
     * 性能报告
     */
    public static class PerformanceReport {
        // 检索指标
        private long totalRetrievals;
        private long cacheHits;
        private double cacheHitRate;
        private double avgRetrievalTime;
        
        // 意图识别指标
        private long totalIntentRecognitions;
        private double ruleBasedRate;
        private double llmBasedRate;
        private double llmCacheHitRate;
        private double avgIntentTime;
        
        // 对话指标
        private long totalChats;
        private double avgChatTime;
        private int activeUsers;
        
        // Getters and Setters
        public long getTotalRetrievals() { return totalRetrievals; }
        public void setTotalRetrievals(long totalRetrievals) { this.totalRetrievals = totalRetrievals; }
        public long getCacheHits() { return cacheHits; }
        public void setCacheHits(long cacheHits) { this.cacheHits = cacheHits; }
        public double getCacheHitRate() { return cacheHitRate; }
        public void setCacheHitRate(double cacheHitRate) { this.cacheHitRate = cacheHitRate; }
        public double getAvgRetrievalTime() { return avgRetrievalTime; }
        public void setAvgRetrievalTime(double avgRetrievalTime) { this.avgRetrievalTime = avgRetrievalTime; }
        public long getTotalIntentRecognitions() { return totalIntentRecognitions; }
        public void setTotalIntentRecognitions(long totalIntentRecognitions) { this.totalIntentRecognitions = totalIntentRecognitions; }
        public double getRuleBasedRate() { return ruleBasedRate; }
        public void setRuleBasedRate(double ruleBasedRate) { this.ruleBasedRate = ruleBasedRate; }
        public double getLlmBasedRate() { return llmBasedRate; }
        public void setLlmBasedRate(double llmBasedRate) { this.llmBasedRate = llmBasedRate; }
        public double getLlmCacheHitRate() { return llmCacheHitRate; }
        public void setLlmCacheHitRate(double llmCacheHitRate) { this.llmCacheHitRate = llmCacheHitRate; }
        public double getAvgIntentTime() { return avgIntentTime; }
        public void setAvgIntentTime(double avgIntentTime) { this.avgIntentTime = avgIntentTime; }
        public long getTotalChats() { return totalChats; }
        public void setTotalChats(long totalChats) { this.totalChats = totalChats; }
        public double getAvgChatTime() { return avgChatTime; }
        public void setAvgChatTime(double avgChatTime) { this.avgChatTime = avgChatTime; }
        public int getActiveUsers() { return activeUsers; }
        public void setActiveUsers(int activeUsers) { this.activeUsers = activeUsers; }
        
        @Override
        public String toString() {
            return String.format(
                "性能报告:\n" +
                "  检索: 总次数=%d, 缓存命中率=%.2f%%, 平均耗时=%.2fms\n" +
                "  意图识别: 总次数=%d, 规则匹配=%.2f%%, LLM=%.2f%%, LLM缓存命中率=%.2f%%, 平均耗时=%.2fms\n" +
                "  对话: 总次数=%d, 平均耗时=%.2fms, 活跃用户=%d",
                totalRetrievals, cacheHitRate, avgRetrievalTime,
                totalIntentRecognitions, ruleBasedRate, llmBasedRate, llmCacheHitRate, avgIntentTime,
                totalChats, avgChatTime, activeUsers
            );
        }
    }
}
