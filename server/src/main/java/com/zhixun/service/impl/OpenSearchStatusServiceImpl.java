package com.zhixun.service.impl;

import com.zhixun.config.OpenSearchConfig;
import com.zhixun.service.OpenSearchStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch.indices.IndexState;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenSearch 状态服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(OpenSearchClient.class)
public class OpenSearchStatusServiceImpl implements OpenSearchStatusService {

    private final OpenSearchClient openSearchClient;
    private final OpenSearchConfig openSearchConfig;

    @Override
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("available", true);
        status.put("hosts", openSearchConfig.getHosts());

        try {
            // 集群健康
            HealthResponse health = openSearchClient.cluster().health();
            Map<String, Object> cluster = new LinkedHashMap<>();
            cluster.put("clusterName", health.clusterName());
            cluster.put("status", health.status().jsonValue());
            cluster.put("numberOfNodes", health.numberOfNodes());
            cluster.put("activeShards", health.activeShards());
            cluster.put("unassignedShards", health.unassignedShards());
            status.put("cluster", cluster);
        } catch (Exception e) {
            status.put("clusterError", e.getMessage());
        }

        // 各索引状态
        Map<String, Object> indices = new LinkedHashMap<>();
        checkIndex(indices, openSearchConfig.getArticleIndex(), "article");
        checkIndex(indices, openSearchConfig.getUserIndex(), "user");
        checkIndex(indices, openSearchConfig.getImageIndex(), "image");
        status.put("indices", indices);

        return status;
    }

    private void checkIndex(Map<String, Object> indices, String indexName, String label) {
        Map<String, Object> indexInfo = new LinkedHashMap<>();
        try {
            boolean exists = openSearchClient.indices().exists(e -> e.index(indexName)).value();
            indexInfo.put("exists", exists);
            if (exists) {
                long docCount = openSearchClient.count(c -> c.index(indexName)).count();
                indexInfo.put("docCount", docCount);

                // 获取索引设置信息
                try {
                    var getResponse = openSearchClient.indices().get(g -> g.index(indexName));
                    Map<String, IndexState> result = getResponse.result();
                    IndexState indexState = result.get(indexName);
                    if (indexState != null && indexState.settings() != null) {
                        indexInfo.put("numberOfShards", indexState.settings().numberOfShards());
                        indexInfo.put("numberOfReplicas", indexState.settings().numberOfReplicas());
                    }
                } catch (Exception e) {
                    log.debug("获取索引设置失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            indexInfo.put("error", e.getMessage());
        }
        indices.put(label, indexInfo);
    }
}
