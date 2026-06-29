package com.zhixun.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * MinIO 配置
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true", matchIfMissing = true)
public class MinioConfig {

    /** MinIO 服务地址 */
    private String endpoint;

    /** 访问密钥 */
    private String accessKey;

    /** 秘密密钥 */
    private String secretKey;

    /** 默认存储桶名称 */
    private String bucketName;

    /**
     * 创建 MinioClient 实例
     * 配置连接超时、写入超时和读取超时，防止 MinIO 不可用时长时间阻塞
     * 创建后自动检查并创建 bucket，防止 bucket 不存在导致所有文件操作失败
     */
    @Bean
    public MinioClient minioClient() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .httpClient(httpClient)
                .build();

        // 启动时自动检查并创建 bucket，防止 MinIO 数据卷丢失后所有文件操作失败
        ensureBucketExists(client);

        return client;
    }

    /**
     * 检查 bucket 是否存在，不存在则自动创建
     * 仅在 MinIO 可达时执行；连接失败时仅记录警告，不影响应用启动
     * （上传时 FileServiceImpl 有本地降级机制）
     */
    private void ensureBucketExists(MinioClient client) {
        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                client.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket '{}' 不存在，已自动创建", bucketName);
            } else {
                log.debug("MinIO bucket '{}' 已存在", bucketName);
            }
        } catch (Exception e) {
            log.warn("MinIO 连接失败或 bucket 检查异常，文件上传将降级到本地存储: {}",
                    e.getMessage());
        }
    }
}
