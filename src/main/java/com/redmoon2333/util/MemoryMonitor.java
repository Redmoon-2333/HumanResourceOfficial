package com.redmoon2333.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内存监控工具类
 * 
 * 用于RAG初始化等内存密集型操作的内存使用监控
 * 在低配服务器（如2核2G）上防止内存溢出导致系统死机
 * 
 * Why: 2核2G服务器在RAG初始化时容易因内存不足而死机
 *      通过主动监控和GC可以避免OOM
 */
public class MemoryMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(MemoryMonitor.class);
    
    private static final Runtime runtime = Runtime.getRuntime();
    
    /**
     * 获取当前内存使用率（0.0 - 1.0）
     * 
     * @return 内存使用率
     */
    public static double getMemoryUsageRatio() {
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return (double) usedMemory / maxMemory;
    }
    
    /**
     * 获取可用内存（MB）
     * 
     * @return 可用内存大小
     */
    public static long getAvailableMemoryMB() {
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long availableMemory = maxMemory - usedMemory;
        
        return availableMemory / (1024 * 1024);
    }
    
    /**
     * 获取已使用内存（MB）
     * 
     * @return 已使用内存大小
     */
    public static long getUsedMemoryMB() {
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return usedMemory / (1024 * 1024);
    }
    
    /**
     * 获取最大可用内存（MB）
     * 
     * @return 最大内存大小
     */
    public static long getMaxMemoryMB() {
        return runtime.maxMemory() / (1024 * 1024);
    }
    
    /**
     * 检查是否内存紧张
     * 
     * @param threshold 内存使用率阈值（0.0 - 1.0）
     * @return 是否超过阈值
     */
    public static boolean isMemoryPressure(double threshold) {
        return getMemoryUsageRatio() >= threshold;
    }
    
    /**
     * 尝试释放内存
     * 主动触发GC并等待一段时间
     * 
     * Warning: 频繁调用GC可能影响性能，仅在内存紧张时使用
     */
    public static void tryReleaseMemory() {
        long beforeMB = getUsedMemoryMB();
        logger.debug("尝试释放内存，当前使用: {}MB", beforeMB);
        
        // 提示GC（注意：这只是建议，JVM可能不会立即执行）
        System.gc();
        
        // 短暂等待GC完成
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long afterMB = getUsedMemoryMB();
        logger.debug("GC完成，当前使用: {}MB，释放: {}MB", afterMB, beforeMB - afterMB);
    }
    
    /**
     * 检查内存并在必要时释放
     * 
     * @param threshold 触发GC的内存使用率阈值
     * @return 释放后是否仍然内存紧张
     */
    public static boolean checkAndReleaseIfNeeded(double threshold) {
        if (isMemoryPressure(threshold)) {
            logger.warn("内存使用率超过阈值 {}%，尝试GC释放内存", (int)(threshold * 100));
            tryReleaseMemory();
            return isMemoryPressure(threshold);
        }
        return false;
    }
    
    /**
     * 打印当前内存状态
     */
    public static void logMemoryStatus() {
        logger.info("内存状态 - 使用: {}MB / 最大: {}MB (使用率: {}%)", 
            getUsedMemoryMB(), 
            getMaxMemoryMB(),
            (int)(getMemoryUsageRatio() * 100));
    }
    
    /**
     * 等待直到内存使用率降低到指定阈值以下
     * 
     * @param threshold 目标内存使用率阈值
     * @param maxWaitMs 最大等待时间（毫秒）
     * @param checkIntervalMs 检查间隔（毫秒）
     * @return 是否成功降低内存使用率
     */
    public static boolean waitForMemoryRelease(double threshold, long maxWaitMs, long checkIntervalMs) {
        long startTime = System.currentTimeMillis();
        
        while (isMemoryPressure(threshold)) {
            if (System.currentTimeMillis() - startTime > maxWaitMs) {
                logger.warn("等待内存释放超时，当前使用率: {}%", (int)(getMemoryUsageRatio() * 100));
                return false;
            }
            
            tryReleaseMemory();
            
            try {
                Thread.sleep(checkIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return true;
    }
}
