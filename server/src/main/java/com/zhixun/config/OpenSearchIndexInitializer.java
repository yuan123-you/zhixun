package com.zhixun.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.stream.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.StringReader;

/**
 * OpenSearch 索引初始化器
 * 应用启动时自动创建所需索引（如不存在）
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(OpenSearchClient.class)
public class OpenSearchIndexInitializer implements CommandLineRunner {

    private final OpenSearchClient openSearchClient;
    private final OpenSearchConfig openSearchConfig;

    @Override
    public void run(String... args) throws Exception {
        try {
            createArticleIndex();
            createUserIndex();
            createImageIndex();
            log.info("OpenSearch 索引初始化完成");
        } catch (Exception e) {
            log.error("OpenSearch 索引初始化失败: {}", e.getMessage());
        }
    }

    /**
     * 创建文章索引
     */
    private void createArticleIndex() throws Exception {
        createIndex(openSearchConfig.getArticleIndex(), "/opensearch/article_mapping.json", "文章");
    }

    /**
     * 创建用户索引
     */
    private void createUserIndex() throws Exception {
        createIndex(openSearchConfig.getUserIndex(), "/opensearch/user_mapping.json", "用户");
    }

    /**
     * 创建图片索引
     */
    private void createImageIndex() throws Exception {
        createIndex(openSearchConfig.getImageIndex(), "/opensearch/image_mapping.json", "图片");
    }

    /**
     * 创建索引（通用方法）
     * 读取 JSON 映射文件，解析 settings 和 mappings，通过 JsonpMapper 反序列化后创建索引
     */
    private void createIndex(String indexName, String mappingResource, String label) throws Exception {
        if (!indexExists(indexName)) {
            try (InputStream is = getClass().getResourceAsStream(mappingResource)) {
                if (is == null) {
                    log.warn("索引映射文件不存在: {}", mappingResource);
                    return;
                }
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(is);
                JsonpMapper jsonpMapper = openSearchClient._transport().jsonpMapper();

                IndexSettings settings = null;
                TypeMapping mappings = null;

                if (root.has("settings")) {
                    String settingsJson = mapper.writeValueAsString(root.get("settings"));
                    try (JsonParser parser = jsonpMapper.jsonProvider().createParser(new StringReader(settingsJson))) {
                        settings = jsonpMapper.deserialize(parser, IndexSettings.class);
                    }
                }
                if (root.has("mappings")) {
                    String mappingsJson = mapper.writeValueAsString(root.get("mappings"));
                    try (JsonParser parser = jsonpMapper.jsonProvider().createParser(new StringReader(mappingsJson))) {
                        mappings = jsonpMapper.deserialize(parser, TypeMapping.class);
                    }
                }

                IndexSettings finalSettings = settings;
                TypeMapping finalMappings = mappings;
                openSearchClient.indices().create(b -> {
                    b.index(indexName);
                    if (finalSettings != null) b.settings(finalSettings);
                    if (finalMappings != null) b.mappings(finalMappings);
                    return b;
                });
                log.info("创建{}索引: {}", label, indexName);
            }
        }
    }

    /**
     * 检查索引是否存在
     */
    private boolean indexExists(String indexName) throws Exception {
        return openSearchClient.indices().exists(
                ExistsRequest.of(b -> b.index(indexName))
        ).value();
    }
}
