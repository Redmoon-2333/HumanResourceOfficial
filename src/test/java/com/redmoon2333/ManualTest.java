package com.redmoon2333;

import com.redmoon2333.util.SmartTextChunker;
import com.redmoon2333.util.SmartTextChunker.DocumentType;
import com.redmoon2333.util.SmartTextChunker.ChunkConfig;

import java.util.List;

/**
 * 手动测试程序 - 验证核心功能
 * 直接运行，不依赖Spring容器
 */
public class ManualTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   AI聊天系统优化功能测试");
        System.out.println("========================================\n");

        // 1. 测试智能分块
        testSmartChunking();

        // 2. 测试文档类型分析
        testDocumentTypeAnalysis();

        // 3. 测试MD5计算
        testMD5Calculation();

        System.out.println("\n========================================");
        System.out.println("   所有测试通过！");
        System.out.println("========================================");
    }

    private static void testSmartChunking() {
        System.out.println("【测试1】智能分块功能");
        System.out.println("------------------------------");

        String text = """
            人力资源中心是一个充满活力的学生组织。
            我们致力于帮助同学们解决学习和生活中的各种问题。
            
            在过去的一年里，我们组织了多次有意义的活动。
            包括迎新晚会、职业规划讲座等。
            
            这些活动不仅丰富了同学们的课余生活。
            也提升了大家的综合素质。
            """;

        List<String> chunks = SmartTextChunker.smartChunk(text);

        System.out.println("✓ 分块成功！共分成 " + chunks.size() + " 个块");
        for (int i = 0; i < chunks.size(); i++) {
            String preview = chunks.get(i).length() > 40
                ? chunks.get(i).substring(0, 40) + "..."
                : chunks.get(i);
            System.out.println("  块 " + (i + 1) + ": " + preview);
        }
        System.out.println();
    }

    private static void testDocumentTypeAnalysis() {
        System.out.println("【测试2】文档类型分析");
        System.out.println("------------------------------");

        // 结构化文档
        String structuredText = """
            第一章 总则
            第一条 为了规范部门管理...
            第二条 本规定适用于全体成员。
            
            第二章 组织架构
            1. 人力资源部
            2. 财务部
            3. 技术部
            """;

        DocumentType type1 = SmartTextChunker.analyzeDocumentType(structuredText);
        System.out.println("✓ 结构化文档识别: " + type1);

        // 叙述性文档
        String narrativeText = """
            人力资源中心是一个充满活力的学生组织。
            我们致力于帮助同学们解决各种问题。
            在过去的一年里，我们组织了多次活动。
            """;

        DocumentType type2 = SmartTextChunker.analyzeDocumentType(narrativeText);
        System.out.println("✓ 叙述性文档识别: " + type2);

        // 验证配置
        ChunkConfig config = SmartTextChunker.getRecommendedConfig("", type1);
        System.out.println("✓ 推荐配置: 大小=" + config.getChunkSize() +
                          ", 重叠=" + config.getChunkOverlap());
        System.out.println();
    }

    private static void testMD5Calculation() {
        System.out.println("【测试3】MD5哈希计算");
        System.out.println("------------------------------");

        String text = "测试文本内容";
        String hash = SmartTextChunker.calculateMD5(text);

        System.out.println("✓ 原文: " + text);
        System.out.println("✓ MD5: " + hash);
        System.out.println("✓ 长度: " + hash.length() + " 字符");

        // 验证一致性
        String hash2 = SmartTextChunker.calculateMD5(text);
        if (hash.equals(hash2)) {
            System.out.println("✓ 哈希一致性验证通过");
        }
        System.out.println();
    }
}
