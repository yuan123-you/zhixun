package com.zhixun.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenSearch 健康检查指示器
 * 集成到 Spring Boot Actuator，在 /actuator/health 中显示 OpenSearch 状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(OpenSearchClient.class)
public class OpenSearchHealthIndicator implements HealthIndicator {

    private final OpenSearchClient openSearchClient;
    private final OpenSearchConfig openSearchConfig;

    @Override
    public Health health() {
        try {
            // 检查集群健康状态
            HealthResponse healthResponse = openSearchClient.cluster().health();
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("clusterName", healthResponse.clusterName());
            details.put("status", healthResponse.status().jsonValue());
            details.put("numberOfNodes", healthResponse.numberOfNodes());
            details.put("activeShards", healthResponse.activeShards());
            details.put("activePrimaryShards", healthResponse.activePrimaryShards());
            details.put("relocatingShards", healthResponse.relocatingShards());
            details.put("unassignedShards", healthResponse.unassignedShards());

            // 检查索引是否存在
            boolean articleIndexExists = openSearchClient.indices()
                    .exists(e -> e.index(openSearchConfig.getArticleIndex())).value();
            boolean userIndexExists = openSearchClient.indices()
                    .exists(e -> e.index(openSearchConfig.getUserIndex())).value();
            boolean imageIndexExists = openSearchClient.indices()
                    .exists(e -> e.index(openSearchConfig.getImageIndex())).value();
            details.put("articleIndexExists", articleIndexExists);
            details.put("userIndexExists", userIndexExists);
            details.put("imageIndexExists", imageIndexExists);

            // 获取各索引文档数
            if (articleIndexExists) {
                long count = openSearchClient.count(c -> c.index(openSearchConfig.getArticleIndex())).count();
                details.put("articleDocCount", count);
            }
            if (userIndexExists) {
                long count = openSearchClient.count(c -> c.index(openSearchConfig.getUserIndex())).count();
                details.put("userDocCount", count);
            }
            if (imageIndexExists) {
                long count = openSearchClient.count(c -> c.index(openSearchConfig.getImageIndex())).count();
                details.put("imageDocCount", count);
            }

            String status = healthResponse.status().jsonValue();
            if ("green".equals(status) || "yellow".equals(status)) {
                return Health.up().withDetails(details).build();
            } else {
                return Health.down().withDetails(details).build();
            }
        } catch (Exception e) {
            log.error("OpenSearch 健康检查失败: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("hosts", openSearchConfig.getHosts())
                    .build();
        }
    }
}
