package com.redmoon2333.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.ai.document.MetadataMode;
import reactor.netty.resources.ConnectionProvider;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class EmbeddingConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${rag.embedding-model:ecnu-embedding-small}")
    private String embeddingModelName;

    @Value("${spring.ai.openai.timeout.connect:30s}")
    private Duration connectTimeout;

    @Value("${spring.ai.openai.timeout.read:300s}")
    private Duration readTimeout;

    @Bean
    public OpenAiApi openAiApi() {
        return OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .embeddingsPath("/v1/embeddings")
                .completionsPath("/v1/chat/completions")
                .webClientBuilder(createWebClientBuilder())
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(OpenAiApi openAiApi) {
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModelName)
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options);
    }

    private WebClient.Builder createWebClientBuilder() {
        // 配置连接池：防止高并发时连接泄漏
        ConnectionProvider connectionProvider = ConnectionProvider.builder("ai-pool")
                .maxConnections(50)                    // 最大连接数
                .maxIdleTime(Duration.ofMinutes(5))    // 空闲连接最大存活时间
                .maxLifeTime(Duration.ofMinutes(30))   // 连接最大生命周期
                .pendingAcquireTimeout(Duration.ofSeconds(60)) // 等待可用连接超时
                .evictInBackground(Duration.ofMinutes(1)) // 后台清理闲置连接间隔
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeout.toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(connectTimeout.toMillis(), TimeUnit.MILLISECONDS)))
                .responseTimeout(readTimeout);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
