package com.zhixun.service.impl;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.config.MinioConfig;
import com.zhixun.service.FileService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传服务实现
 * 优先使用 MinIO 对象存储，不可用时自动降级到本地存储
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true", matchIfMissing = true)
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /** 本地存储根目录 */
    private static final String LOCAL_UPLOAD_DIR = "./uploads";

    /** MinIO 是否可用（运行时检测） */
    private volatile boolean minioAvailable = true;
    private volatile long lastMinioCheckTime = 0;
    private static final long MINIO_CHECK_INTERVAL_MS = 60_000; // 每60秒重新检测一次

    /**
     * 启动时检测 MinIO 是否可用
     */
    @PostConstruct
    public void init() {
        minioAvailable = checkMinioAvailable();
        if (minioAvailable) {
            log.info("MinIO 存储服务连接成功，使用 MinIO 存储文件");
        } else {
            log.warn("MinIO 存储服务不可用，将使用本地存储作为降级方案");
        }
    }

    /** 图片允许的扩展名 */
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp"
    );

    /** 图片文件头魔数 */
    private static final Map<String, byte[]> FILE_MAGIC_NUMBERS = Map.of(
            "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8},
            "jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8},
            "png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "gif", new byte[]{0x47, 0x49, 0x46, 0x38},
            "webp", new byte[]{0x52, 0x49, 0x46, 0x46}
    );

    /** 图片最大大小（5MB） */
    private static final long IMAGE_MAX_SIZE = 5 * 1024 * 1024;

    /** 附件最大大小（20MB） */
    private static final long FILE_MAX_SIZE = 20 * 1024 * 1024;

    /** 语音最大大小（10MB） */
    private static final long VOICE_MAX_SIZE = 10 * 1024 * 1024;

    /** 语音允许的扩展名 */
    private static final Map<String, String> VOICE_EXTENSIONS = Map.of(
            "webm", "audio/webm",
            "wav", "audio/wav",
            "mp3", "audio/mpeg",
            "m4a", "audio/mp4",
            "ogg", "audio/ogg"
    );

    @Override
    public String uploadImage(MultipartFile file) {
        // 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }

        // 校验文件大小
        if (file.getSize() > IMAGE_MAX_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, "图片大小不能超过5MB");
        }

        // 获取文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (!StringUtils.hasText(extension) || !IMAGE_EXTENSIONS.containsKey(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持 jpg/png/gif/webp 格式图片");
        }

        // 文件头魔数校验
        validateFileMagicNumber(file, extension.toLowerCase());

        // 生成文件路径
        String filePath = generateFilePath("images", extension);

        // 上传文件（优先 MinIO，失败降级本地）
        return uploadWithFallback(file, filePath, IMAGE_EXTENSIONS.get(extension.toLowerCase()), "images");
    }

    @Override
    public String uploadFile(MultipartFile file) {
        // 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }

        // 校验文件大小
        if (file.getSize() > FILE_MAX_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, "附件大小不能超过20MB");
        }

        // 获取文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (!StringUtils.hasText(extension)) {
            extension = "bin";
        }

        // 生成文件路径
        String filePath = generateFilePath("files", extension);

        // 上传文件（优先 MinIO，失败降级本地）
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            contentType = "application/octet-stream";
        }
        return uploadWithFallback(file, filePath, contentType, "files");
    }

    @Override
    public String uploadVoice(MultipartFile file) {
        // 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "语音文件不能为空");
        }

        // 校验文件大小
        if (file.getSize() > VOICE_MAX_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, "语音文件大小不能超过10MB");
        }

        // 获取文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        if (!StringUtils.hasText(extension) || !VOICE_EXTENSIONS.containsKey(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持 webm/wav/mp3/m4a/ogg 格式语音");
        }

        // 生成文件路径
        String filePath = generateFilePath("voices", extension);

        // 上传文件（优先 MinIO，失败降级本地）
        return uploadWithFallback(file, filePath, VOICE_EXTENSIONS.get(extension.toLowerCase()), "voices");
    }

    // ========== 核心：带降级的上传逻辑 ==========

    /**
     * 上传文件，优先使用 MinIO，失败时自动降级到本地存储
     *
     * @param file        文件
     * @param objectPath  对象路径（MinIO用）
     * @param contentType 内容类型
     * @param subDir      本地存储子目录（如 "images"/"files"/"voices"）
     * @return 文件访问URL
     */
    private String uploadWithFallback(MultipartFile file, String objectPath, String contentType, String subDir) {
        // 如果 MinIO 之前检测为不可用，尝试重新检测
        if (!minioAvailable && shouldRetryMinio()) {
            minioAvailable = checkMinioAvailable();
        }

        if (minioAvailable) {
            try {
                return uploadToMinio(file, objectPath, contentType);
            } catch (Exception e) {
                log.warn("MinIO 上传失败，降级到本地存储: {}", e.getMessage());
                minioAvailable = false;
                lastMinioCheckTime = System.currentTimeMillis();
                // 降级到本地存储
                return saveToLocal(file, subDir);
            }
        } else {
            log.debug("MinIO 不可用，直接使用本地存储");
            return saveToLocal(file, subDir);
        }
    }

    /**
     * 上传文件到 MinIO
     */
    private String uploadToMinio(MultipartFile file, String objectPath, String contentType) {
        String bucketName = minioConfig.getBucketName();

        // 确保存储桶存在
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build());
        if (!bucketExists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build());
        }

        // 上传文件
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectPath)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(contentType)
                        .build());

        log.debug("MinIO 上传成功: {}", objectPath);

        // 返回 MinIO 文件访问URL
        return minioConfig.getEndpoint() + "/" + bucketName + "/" + objectPath;
    }

    /**
     * 保存文件到本地存储
     */
    private String saveToLocal(MultipartFile file, String subDir) {
        try {
            Path uploadPath = Paths.get(LOCAL_UPLOAD_DIR, subDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            Path filePath = uploadPath.resolve(newFilename);
            file.transferTo(filePath.toFile());

            log.info("本地存储上传成功: {}", filePath);
            return "/api/uploads/" + subDir + "/" + newFilename;
        } catch (IOException e) {
            log.error("本地存储写入失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件上传失败：存储服务不可用");
        }
    }

    /**
     * 检测 MinIO 是否可用
     */
    private boolean checkMinioAvailable() {
        try {
            minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build());
            log.info("MinIO 连接恢复，切换回 MinIO 存储");
            return true;
        } catch (Exception e) {
            log.debug("MinIO 仍不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 判断是否应该重新检测 MinIO
     */
    private boolean shouldRetryMinio() {
        return System.currentTimeMillis() - lastMinioCheckTime > MINIO_CHECK_INTERVAL_MS;
    }

    // ========== 内部方法 ==========

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 校验文件头魔数
     */
    private void validateFileMagicNumber(MultipartFile file, String extension) {
        try (InputStream is = file.getInputStream()) {
            byte[] magicNumber = FILE_MAGIC_NUMBERS.get(extension);
            if (magicNumber != null) {
                byte[] header = new byte[magicNumber.length];
                int read = is.read(header);
                if (read < magicNumber.length) {
                    throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件内容与扩展名不匹配");
                }

                // 比较文件头
                for (int i = 0; i < magicNumber.length; i++) {
                    if (header[i] != magicNumber[i]) {
                        throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件内容与扩展名不匹配");
                    }
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件头校验失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件校验失败");
        }
    }

    /**
     * 生成文件存储路径
     * 格式: {type}/{yyyy/MM/dd}/{uuid}.{extension}
     */
    private String generateFilePath(String type, String extension) {
        LocalDate now = LocalDate.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        return type + "/" + datePath + "/" + fileName;
    }
}
