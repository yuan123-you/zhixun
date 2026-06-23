package com.zhixun.config;

import lombok.Data;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * OpenSearch 配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "opensearch")
@ConditionalOnProperty(name = "opensearch.enabled", havingValue = "true", matchIfMissing = true)
public class OpenSearchConfig {

    /** OpenSearch 主机地址列表（逗号分隔） */
    private String hosts = "http://localhost:9200";

    /** 用户名 */
    private String username = "admin";

    /** 密码 */
    private String password = "admin";

    /** 连接超时（毫秒） */
    private int connectTimeout = 5000;

    /** Socket 超时（毫秒） */
    private int socketTimeout = 60000;

    /** 索引名前缀 */
    private String indexPrefix = "zhixun_";

    /** 同义词文件路径（classpath 路径） */
    private String synonymFilePath = "opensearch/synonyms.txt";

    /**
     * 文章索引名
     */
    public String getArticleIndex() {
        return indexPrefix + "article";
    }

    /**
     * 用户索引名
     */
    public String getUserIndex() {
        return indexPrefix + "user";
    }

    /**
     * 图片索引名
     */
    public String getImageIndex() {
        return indexPrefix + "image";
    }

    @Bean
    public OpenSearchClient openSearchClient() {
        // 解析主机列表
        List<HttpHost> httpHosts = Arrays.stream(hosts.split(","))
                .map(String::trim)
                .map(host -> {
                    String[] parts = host.replace("http://", "").replace("https://", "").split(":");
                    String hostname = parts[0];
                    int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
                    String scheme = host.startsWith("https://") ? "https" : "http";
                    return new HttpHost(hostname, port, scheme);
                })
                .toList();

        // 认证信息（仅当用户名和密码都非空时启用）
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
        }

        RestClientBuilder builder = RestClient.builder(httpHosts.toArray(new HttpHost[0]))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                        httpClientBuilder = httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                    return httpClientBuilder;
                })
                .setRequestConfigCallback(requestConfigBuilder ->
                        requestConfigBuilder
                                .setConnectTimeout(connectTimeout)
                                .setSocketTimeout(socketTimeout)
                );

        OpenSearchTransport transport = new RestClientTransport(builder.build(), new JacksonJsonpMapper());
        return new OpenSearchClient(transport);
    }
}
