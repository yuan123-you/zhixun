package com.zhixun.service.impl;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.config.MinioConfig;
import com.zhixun.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * MinIO 上传失败时是否降级到本地
     * 每次上传都先尝试 MinIO，失败时立即降级本地，避免复杂的健康检查与节流逻辑
     */
    private static final boolean FALLBACK_TO_LOCAL = true;

    /** 图片允许的扩展名 */
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp",
            "bmp", "image/bmp",
            "svg", "image/svg+xml"
    );

    /** 图片文件头魔数 */
    private static final Map<String, byte[]> FILE_MAGIC_NUMBERS = Map.of(
            "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8},
            "jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8},
            "png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "gif", new byte[]{0x47, 0x49, 0x46, 0x38},
            "webp", new byte[]{0x52, 0x49, 0x46, 0x46},
            "bmp", new byte[]{0x42, 0x4D}
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
        try {
            // 校验文件是否为空
            if (file == null || file.isEmpty()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要上传的图片文件");
            }

            // 校验文件大小
            if (file.getSize() > IMAGE_MAX_SIZE) {
                throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, "图片大小不能超过5MB，当前文件大小: " + formatFileSize(file.getSize()));
            }

            // 获取并校验原始文件名
            String originalFilename = file.getOriginalFilename();
            if (!StringUtils.hasText(originalFilename)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名不能为空");
            }

            // 文件名安全校验：防止路径遍历攻击
            String safeFilename = sanitizeFilename(originalFilename);
            if (!StringUtils.hasText(safeFilename)) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件名不合法");
            }

            // 获取文件扩展名
            String extension = getFileExtension(safeFilename);
            if (!StringUtils.hasText(extension) || !IMAGE_EXTENSIONS.containsKey(extension.toLowerCase())) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "仅支持 jpg/png/gif/webp/bmp 格式图片");
            }

            // 校验 Content-Type
            String contentType = file.getContentType();
            if (StringUtils.hasText(contentType) && !contentType.startsWith("image/")) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件 Content-Type 不是图片类型: " + contentType);
            }

            // 文件头魔数校验
            validateFileMagicNumber(file, extension.toLowerCase());

            // 生成文件路径
            String filePath = generateFilePath("images", extension);

            log.info("图片上传请求: fileName={}, size={}, extension={}", safeFilename, formatFileSize(file.getSize()), extension);

            // 上传文件（优先 MinIO，失败降级本地）
            return uploadWithFallback(file, filePath, IMAGE_EXTENSIONS.get(extension.toLowerCase()), "images");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("图片上传未知异常: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "图片上传失败，请稍后重试");
        }
    }

    @Override
    public String uploadFile(MultipartFile file) throws Exception {
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
    public String uploadVoice(MultipartFile file) throws Exception {
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
    private String uploadWithFallback(MultipartFile file, String objectPath, String contentType, String subDir) throws Exception {
        if (FALLBACK_TO_LOCAL) {
            try {
                return uploadToMinio(file, objectPath, contentType);
            } catch (Exception e) {
                // MinIO 任何异常（连接失败、桶不存在、签名错误等）都立即降级到本地，
                // 避免复杂的健康检查/节流策略影响首屏体验
                log.warn("MinIO 上传失败，降级到本地存储 [{}]: {}", subDir, e.getMessage());
                return saveToLocal(file, subDir);
            }
        }
        return uploadToMinio(file, objectPath, contentType);
    }

    /**
     * 上传文件到 MinIO
     * 任何异常向上抛出，由 uploadWithFallback 决定是否降级到本地
     */
    private String uploadToMinio(MultipartFile file, String objectPath, String contentType) throws Exception {
        String bucketName = minioConfig.getBucketName();

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(contentType)
                            .build());
        }

        log.debug("MinIO 上传成功: {}", objectPath);

        // 返回服务器代理路径：避免 MinIO 在私有桶/无签名时返回 307 触发重定向循环
        // 浏览器通过 /api/v1/files/view/{bucket}/{objectKey} 访问
        // 由 FileController 的 view 接口从 MinIO 读取后流式返回给客户端
        return buildProxiedUrl(bucketName, objectPath);
    }

    @Override
    public InputStream readFile(String bucket, String objectKey) {
        try {
            return minioClient.getObject(
                    io.minio.GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());
        } catch (Exception e) {
            log.warn("MinIO 读取失败, bucket={}, objectKey={}: {}", bucket, objectKey, e.getMessage());
            return null;
        }
    }

    /**
     * 构建服务器代理访问 URL
     * 浏览器通过该 URL 访问文件时由后端从 MinIO 读取并流式返回
     * 这样可以避免 MinIO 在对象无签名时返回 307 重定向到控制台或签名 URL 触发重定向循环
     */
    private String buildProxiedUrl(String bucket, String objectKey) {
        return "/api/v1/files/view/" + bucket + "/" + objectKey;
    }

    /**
     * 保存文件到本地存储
     * 与 viewFile 读取侧的本地降级布局保持一致：./uploads/{subDir}/{yyyy/MM/dd}/{uuid}.ext
     *
     * 注意：使用 {@code Files.copy(InputStream, Path, REPLACE_EXISTING)} 替代
     * {@code MultipartFile.transferTo(File)}。原因：
     * 1. {@code validateFileMagicNumber()} 中已通过 getInputStream() 读取并关闭了流，
     *    StandardMultipartFile 在底层是基于临时文件实现的，虽然 getInputStream() 每次
     *    返回新流，但 Spring 在部分版本下会复用底层临时文件，transferTo 移动后 multipart
     *    缓存失效，再次 transferTo 会抛 IllegalStateException。
     * 2. Files.copy 每次基于当前 getInputStream() 的新流写入，更稳健。
     */
    private String saveToLocal(MultipartFile file, String subDir) {
        try {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path uploadPath = Paths.get(LOCAL_UPLOAD_DIR, subDir, datePath);
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
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("本地存储上传成功: {}", filePath);
            // 返回 URL：bucket=subDir, objectKey=datePath/filename，
            // 与 viewFile 读取时的"布局 1"（./uploads/{bucket}/{key}）匹配
            return "/api/v1/files/view/" + subDir + "/" + datePath + "/" + newFilename;
        } catch (IOException e) {
            log.error("本地存储写入失败 [subDir={}, size={}]: {}", subDir, file.getSize(), e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件上传失败：存储服务不可用");
        }
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
     * 文件名安全处理：移除路径分隔符，防止路径遍历攻击
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "";
        // 移除路径分隔符和非法字符，只保留文件名部分
        String name = filename.replaceAll(".*[/\\\\]", "");
        // 移除控制字符
        name = name.replaceAll("[\\x00-\\x1F\\x7F]", "");
        // 限制文件名长度
        if (name.length() > 255) {
            name = name.substring(0, 255);
        }
        return name.trim();
    }

    /**
     * 格式化文件大小显示
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * 校验文件头魔数
     * 使用 mark/reset 机制避免消耗 InputStream，确保后续上传操作能正常读取文件内容
     */
    private void validateFileMagicNumber(MultipartFile file, String extension) {
        byte[] magicNumber = FILE_MAGIC_NUMBERS.get(extension);
        if (magicNumber == null) {
            return;
        }

        InputStream is = null;
        try {
            is = file.getInputStream();
            if (!is.markSupported()) {
                // 如果流不支持 mark/reset，则读取字节数组后重新创建流
                // 对于 Spring 的 StandardMultipartFile，getInputStream() 每次返回新流，无需特殊处理
                byte[] header = new byte[magicNumber.length];
                int read = is.read(header);
                if (read < magicNumber.length) {
                    throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件内容与扩展名不匹配");
                }
                for (int i = 0; i < magicNumber.length; i++) {
                    if (header[i] != magicNumber[i]) {
                        throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件内容与扩展名不匹配");
                    }
                }
                return;
            }

            // 支持 mark 的流：标记位置，读取后重置
            is.mark(magicNumber.length);
            byte[] header = new byte[magicNumber.length];
            int read = is.read(header);
            if (read < magicNumber.length) {
                throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件内容与扩展名不匹配");
            }
            for (int i = 0; i < magicNumber.length; i++) {
                if (header[i] != magicNumber[i]) {
                    throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "文件内容与扩展名不匹配");
                }
            }
            is.reset();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件头校验失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件校验失败");
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException ignored) {}
            }
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
