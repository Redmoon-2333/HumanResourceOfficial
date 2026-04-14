package com.redmoon2333.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@ConditionalOnExpression("'${aliyun.oss.accessKeyId:}' != '' && '${aliyun.oss.accessKeySecret:}' != ''")
public class OssConfig implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(OssConfig.class);

    @Value("${aliyun.oss.endpoint:oss-cn-beijing.aliyuncs.com}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId:}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret:}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucketName:}")
    private String bucketName;

    private OSS ossClient;

    @Bean
    public OSS ossClient() {
        logger.info("OSS配置信息: endpoint={}, bucketName={}", endpoint, bucketName);

        if (accessKeyId == null || accessKeyId.isEmpty() ||
            accessKeySecret == null || accessKeySecret.isEmpty()) {
            logger.warn("OSS凭证未正确配置，将返回null客户端");
            return null;
        }

        try {
            if (ossClient == null) {
                ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
                logger.info("OSS客户端创建成功");
            }
            return ossClient;
        } catch (Exception e) {
            logger.error("创建OSS客户端时发生异常", e);
            return null;
        }
    }

    public String getBucketName() {
        return bucketName;
    }

    @Override
    public void destroy() throws Exception {
        if (ossClient != null) {
            ossClient.shutdown();
            logger.info("OSS客户端已关闭");
        }
    }
}
