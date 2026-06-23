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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
    private final StringRedisTemplate stringRedisTemplate;

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
     * 自动将 synonyms_path 替换为内联 synonyms，确保同义词分析器正确创建
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

                // 将 synonyms_path 替换为内联 synonyms，确保同义词分析器正确创建
                injectInlineSynonyms(root, mapper);

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
     * 从 classpath 加载同义词文件，将 synonyms_path 替换为内联 synonyms
     * 这样无需将同义词文件部署到 OpenSearch 服务器上
     */
    private void injectInlineSynonyms(JsonNode root, ObjectMapper mapper) {
        try {
            JsonNode analysis = root.path("settings").path("analysis");
            JsonNode filters = analysis.path("filter");
            if (filters.isObject()) {
                List<String> inlineSynonyms = loadSynonyms();
                if (inlineSynonyms.isEmpty()) return;

                for (java.util.Iterator<java.util.Map.Entry<String, JsonNode>> it = filters.fields(); it.hasNext(); ) {
                    java.util.Map.Entry<String, JsonNode> entry = it.next();
                    JsonNode filterNode = entry.getValue();
                    if (filterNode.isObject() && "synonym".equals(filterNode.path("type").asText())) {
                        com.fasterxml.jackson.databind.node.ObjectNode objNode = (com.fasterxml.jackson.databind.node.ObjectNode) filterNode;
                        objNode.remove("synonyms_path");
                        objNode.putArray("synonyms").addAll(
                                inlineSynonyms.stream()
                                        .map(com.fasterxml.jackson.databind.node.TextNode::new)
                                        .collect(java.util.stream.Collectors.toList())
                        );
                        log.info("已将同义词文件内容内联注入到索引设置中，共 {} 条同义词规则", inlineSynonyms.size());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("加载同义词文件失败，将使用 synonyms_path 配置: {}", e.getMessage());
        }
    }

    private List<String> loadSynonyms() {
        List<String> redisSynonyms = getSynonymsFromRedis();
        if (redisSynonyms != null) {
            log.info("从 Redis 缓存加载同义词，共 {} 条规则", redisSynonyms.size());
            return redisSynonyms;
        }
        return loadSynonymsFromClasspath();
    }

    private List<String> getSynonymsFromRedis() {
        try {
            List<String> cached = stringRedisTemplate.opsForList().range("synonym:list", 0, -1);
            if (cached != null && !cached.isEmpty()) {
                return new ArrayList<>(cached);
            }
        } catch (Exception e) {
            log.warn("从 Redis 获取同义词失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从 classpath 加载同义词文件内容
     */
    private List<String> loadSynonymsFromClasspath() {
        List<String> synonyms = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream("/" + openSearchConfig.getSynonymFilePath())) {
            if (is == null) {
                log.warn("同义词文件不存在: {}", openSearchConfig.getSynonymFilePath());
                return synonyms;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // 跳过空行和注释行
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        synonyms.add(line);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("读取同义词文件失败: {}", e.getMessage());
        }
        return synonyms;
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
