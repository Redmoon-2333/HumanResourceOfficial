package com.redmoon2333;

import com.redmoon2333.service.*;
import com.redmoon2333.util.SmartTextChunker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 集成测试程序
 * 启动Spring Boot应用并测试所有新功能
 * 
 * 运行方式：
 * 1. 确保Redis已启动
 * 2. 确保配置文件正确
 * 3. 运行：mvn spring-boot:run -Dspring-boot.run.main-class=com.redmoon2333.IntegrationTest
 */
@SpringBootApplication
public class IntegrationTest {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationTest.class, args);
    }

    @Bean
    public CommandLineRunner run(
            HybridIntentRecognizer intentRecognizer,
            SmartQueryOptimizer queryOptimizer,
            RagCacheService cacheService,
            PerformanceMonitorService monitorService) {
        return args -> {
            System.out.println("\n========================================");
            System.out.println("   AI聊天系统 - 集成测试开始");
            System.out.println("========================================\n");

            // 1. 测试智能分块
            testSmartChunking();

            // 2. 测试混合意图识别
            testIntentRecognition(intentRecognizer);

            // 3. 测试查询优化
            testQueryOptimization(queryOptimizer);

            // 4. 测试性能监控
            testPerformanceMonitor(monitorService);

            System.out.println("\n========================================");
            System.out.println("   所有集成测试通过！");
            System.out.println("========================================\n");

            // 输出性能报告
            PerformanceMonitorService.PerformanceReport report = monitorService.getReport();
            System.out.println(report);

            // 退出应用
            System.exit(0);
        };
    }

    private static void testSmartChunking() {
        System.out.println("【测试1】智能分块功能");
        System.out.println("------------------------------");

        String text = """
            人力资源中心是一个充满活力的学生组织。
            我们致力于帮助同学们解决学习和生活中的各种问题。
            
            在过去的一年里，我们组织了多次有意义的活动。
            包括迎新晚会、职业规划讲座等。
            """;

        List<String> chunks = SmartTextChunker.smartChunk(text);
        System.out.println("✓ 分块成功！共分成 " + chunks.size() + " 个块");
        
        for (int i = 0; i < Math.min(chunks.size(), 3); i++) {
            String preview = chunks.get(i).length() > 40
                ? chunks.get(i).substring(0, 40) + "..."
                : chunks.get(i);
            System.out.println("  块 " + (i + 1) + ": " + preview);
        }
        System.out.println();
    }

    private static void testIntentRecognition(HybridIntentRecognizer recognizer) {
        System.out.println("【测试2】混合意图识别");
        System.out.println("------------------------------");

        String[] testQueries = {
            "什么是人力资源中心？",
            "图书馆在哪里？",
            "如何申请活动场地？",
            "你好！",
            "请问活动策划需要什么材料？"
        };

        for (String query : testQueries) {
            HybridIntentRecognizer.IntentResult result = recognizer.recognizeFast(query);
            System.out.println("  查询: " + query);
            System.out.println("  → 意图: " + result.getIntent() + 
                             " (方法: " + result.getMethod() + 
                             ", 置信度: " + String.format("%.2f", result.getConfidence()) + ")");
            System.out.println();
        }
    }

    private static void testQueryOptimization(SmartQueryOptimizer optimizer) {
        System.out.println("【测试3】查询优化");
        System.out.println("------------------------------");

        String[] testQueries = {
            "请问什么是活动策划？",
            "如何评价这个活动？",
            "告诉我图书馆的位置"
        };

        for (String query : testQueries) {
            String optimized = optimizer.optimizeQuery(query);
            SmartQueryOptimizer.QueryIntent intent = optimizer.recognizeIntent(query);
            System.out.println("  原查询: " + query);
            System.out.println("  → 意图: " + intent);
            System.out.println("  → 优化后: " + optimized);
            System.out.println();
        }
    }

    private static void testPerformanceMonitor(PerformanceMonitorService monitor) {
        System.out.println("【测试4】性能监控");
        System.out.println("------------------------------");

        // 模拟一些操作记录
        monitor.recordRetrieval(50, true);
        monitor.recordRetrieval(200, false);
        monitor.recordIntentRecognition(1, "rule");
        monitor.recordIntentRecognition(500, "llm");
        monitor.recordChat("user_1", 1500);

        PerformanceMonitorService.PerformanceReport report = monitor.getReport();
        System.out.println("✓ 性能监控数据记录成功");
        System.out.println("  - 检索次数: " + report.getTotalRetrievals());
        System.out.println("  - 缓存命中率: " + String.format("%.2f", report.getCacheHitRate()) + "%");
        System.out.println("  - 意图识别次数: " + report.getTotalIntentRecognitions());
        System.out.println("  - 规则匹配率: " + String.format("%.2f", report.getRuleBasedRate()) + "%");
        System.out.println();
    }
}
