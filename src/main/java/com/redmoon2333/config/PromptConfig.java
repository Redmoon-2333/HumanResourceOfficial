package com.redmoon2333.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 提示词配置管理类
 * Why: 将硬编码的提示词迁移到配置文件，支持集中管理和热更新
 */
@Component
public class PromptConfig {
    private static final Logger logger = LoggerFactory.getLogger(PromptConfig.class);

    @Value("classpath:/prompttemplate/system-prompts.yml")
    private Resource promptsResource;

    private Map<String, Object> prompts;

    @PostConstruct
    public void init() {
        loadPrompts();
    }

    /**
     * 加载提示词配置
     */
    public void loadPrompts() {
        try {
            Yaml yaml = new Yaml();
            String content = new String(promptsResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            prompts = yaml.load(content);
            logger.info("提示词配置加载成功");
        } catch (IOException e) {
            logger.error("加载提示词配置失败", e);
            throw new RuntimeException("无法加载提示词配置", e);
        }
    }

    /**
     * 获取基础系统提示词
     */
    public String getSystemPrompt() {
        return getPrompt("system", "base");
    }

    /**
     * 获取带工具的系统提示词
     */
    public String getSystemPromptWithTools() {
        return getPrompt("system", "with_tools");
    }

    /**
     * 获取策划案生成提示词
     */
    public String getPlanGeneratorPrompt() {
        return getPrompt("plan_generator");
    }

    /**
     * 获取RAG上下文模板
     */
    public String getRagContextTemplate() {
        return getPrompt("rag", "context_template");
    }

    /**
     * 通用获取提示词方法
     */
    @SuppressWarnings("unchecked")
    private String getPrompt(String... keys) {
        if (prompts == null) {
            loadPrompts();
        }

        Object current = prompts;
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            } else {
                logger.warn("提示词路径不存在: {}", String.join(".", keys));
                return "";
            }
        }

        return current != null ? current.toString() : "";
    }

    /**
     * 重新加载提示词（支持热更新）
     */
    public void reload() {
        loadPrompts();
        logger.info("提示词配置已重新加载");
    }
}
