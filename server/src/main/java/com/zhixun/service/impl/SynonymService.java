package com.zhixun.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.config.OpenSearchConfig;
import jakarta.json.stream.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.opensearch.client.RestClient;
import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(OpenSearchClient.class)
public class SynonymService {

    private static final String REDIS_KEY = "synonym:list";
    private static final long REDIS_TTL_MINUTES = 30;

    private final OpenSearchClient openSearchClient;
    private final OpenSearchConfig openSearchConfig;
    private final StringRedisTemplate stringRedisTemplate;

    public List<String> getAllSynonyms() {
        List<String> cached = getSynonymsFromRedis();
        if (cached != null) {
            return cached;
        }
        List<String> synonyms = loadSynonymsFromClasspath();
        saveSynonymsToRedis(synonyms);
        return synonyms;
    }

    public void addSynonym(String synonymRule) {
        List<String> synonyms = getAllSynonyms();
        if (!synonyms.contains(synonymRule)) {
            synonyms.add(synonymRule);
            saveSynonymsToRedis(synonyms);
            log.info("添加同义词规则: {}", synonymRule);
        }
    }

    public void removeSynonym(String synonymRule) {
        List<String> synonyms = getAllSynonyms();
        if (synonyms.remove(synonymRule)) {
            saveSynonymsToRedis(synonyms);
            log.info("删除同义词规则: {}", synonymRule);
        }
    }

    public boolean reloadSearchAnalyzers() {
        try {
            String indexName = openSearchConfig.getArticleIndex();
            RestClientTransport transport = (RestClientTransport) openSearchClient._transport();
            RestClient restClient = transport.restClient();
            Request request = new Request("POST", "/" + indexName + "/_reload_search_analyzers");
            Response response = restClient.performRequest(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                log.info("同义词热更新成功（reload_search_analyzers）, index={}", indexName);
                return true;
            } else {
                log.warn("reload_search_analyzers 返回非成功状态码: {}", statusCode);
                return false;
            }
        } catch (Exception e) {
            log.warn("reload_search_analyzers 不支持或执行失败: {}", e.getMessage());
            return false;
        }
    }

    public boolean rebuildIndexWithSynonyms() {
        try {
            String aliasName = openSearchConfig.getArticleIndex();
            String newIndex = aliasName + "_" + Instant.now().toEpochMilli();

            createIndexWithSynonyms(newIndex);
            log.info("创建新索引: {}", newIndex);

            if (indexExists(aliasName)) {
                openSearchClient.reindex(b -> b
                        .source(s -> s.index(aliasName))
                        .dest(d -> d.index(newIndex))
                );
                log.info("数据从 {} 迁移到 {} 完成", aliasName, newIndex);

                openSearchClient.indices().delete(DeleteIndexRequest.of(b -> b.index(aliasName)));
                log.info("删除旧索引: {}", aliasName);
            }

            openSearchClient.indices().putAlias(b -> b
                    .index(newIndex)
                    .name(aliasName)
            );
            log.info("创建别名 {} -> {}", aliasName, newIndex);

            log.info("同义词索引重建完成");
            return true;
        } catch (Exception e) {
            log.error("重建索引失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private void createIndexWithSynonyms(String indexName) throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/opensearch/article_mapping.json")) {
            if (is == null) {
                throw new IllegalStateException("索引映射文件不存在: /opensearch/article_mapping.json");
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);

            injectInlineSynonyms(root);

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
        }
    }

    private void injectInlineSynonyms(JsonNode root) {
        try {
            JsonNode analysis = root.path("settings").path("analysis");
            JsonNode filters = analysis.path("filter");
            if (filters.isObject()) {
                List<String> inlineSynonyms = getAllSynonyms();
                if (inlineSynonyms.isEmpty()) return;

                for (java.util.Iterator<java.util.Map.Entry<String, JsonNode>> it = filters.fields(); it.hasNext(); ) {
                    java.util.Map.Entry<String, JsonNode> entry = it.next();
                    JsonNode filterNode = entry.getValue();
                    if (filterNode.isObject() && "synonym".equals(filterNode.path("type").asText())) {
                        com.fasterxml.jackson.databind.node.ObjectNode objNode =
                                (com.fasterxml.jackson.databind.node.ObjectNode) filterNode;
                        objNode.remove("synonyms_path");
                        objNode.putArray("synonyms").addAll(
                                inlineSynonyms.stream()
                                        .map(com.fasterxml.jackson.databind.node.TextNode::new)
                                        .collect(Collectors.toList())
                        );
                        log.info("已将同义词内容内联注入到索引设置中，共 {} 条同义词规则", inlineSynonyms.size());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("注入内联同义词失败: {}", e.getMessage());
        }
    }

    private List<String> getSynonymsFromRedis() {
        try {
            List<String> cached = stringRedisTemplate.opsForList().range(REDIS_KEY, 0, -1);
            if (cached != null && !cached.isEmpty()) {
                return new ArrayList<>(cached);
            }
        } catch (Exception e) {
            log.warn("从 Redis 获取同义词失败: {}", e.getMessage());
        }
        return null;
    }

    private void saveSynonymsToRedis(List<String> synonyms) {
        try {
            stringRedisTemplate.delete(REDIS_KEY);
            if (!synonyms.isEmpty()) {
                stringRedisTemplate.opsForList().rightPushAll(REDIS_KEY, synonyms);
            }
            stringRedisTemplate.expire(REDIS_KEY, REDIS_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("保存同义词到 Redis 失败: {}", e.getMessage());
        }
    }

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

    private boolean indexExists(String indexName) throws Exception {
        return openSearchClient.indices().exists(
                ExistsRequest.of(b -> b.index(indexName))
        ).value();
    }
}
