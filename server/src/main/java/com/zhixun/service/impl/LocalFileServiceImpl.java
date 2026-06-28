package com.zhixun.service.impl;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.service.FileService;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 本地文件存储实现（当MinIO被显式禁用时使用）
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "minio.enabled", havingValue = "false")
public class LocalFileServiceImpl implements FileService {

    private static final String UPLOAD_DIR = "./uploads";
    private static final String IMAGE_DIR = "/images";
    private static final String FILE_DIR = "/files";
    private static final String VOICE_DIR = "/voices";

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

        // 文件名安全校验
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

        log.info("图片上传请求: fileName={}, size={}, extension={}", safeFilename, formatFileSize(file.getSize()), extension);

        return saveFile(file, IMAGE_DIR);
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

        return saveFile(file, FILE_DIR);
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

        return saveFile(file, VOICE_DIR);
    }

    /**
     * 从本地存储读取文件流
     * 本实现将 {@code bucket} 视作子目录（{@code images / files / voices}），
     * {@code objectKey} 视作相对子目录的文件名，两者拼接后在 {@link #UPLOAD_DIR} 下查找文件。
     * 同时做路径遍历校验，防止越权访问上传目录之外的文件。
     *
     * @param bucket    子目录名（如 images/files/voices）
     * @param objectKey 对象 key（相对路径）
     * @return 文件输入流；不存在时返回 null
     */
    @Override
    public InputStream readFile(String bucket, String objectKey) {
        if (!StringUtils.hasText(bucket) || !StringUtils.hasText(objectKey)) {
            return null;
        }
        try {
            // 清理 bucket/objectKey，防止路径分隔符注入
            String safeBucket = bucket.replaceAll(".*[/\\\\]", "").trim();
            String safeKey = objectKey.replace("\\", "/");
            // 禁止向上跳目录
            if (safeKey.contains("..")) {
                log.warn("本地文件读取拒绝，路径包含 '..': bucket={}, objectKey={}", bucket, objectKey);
                return null;
            }
            // 取文件名（防止 objectKey 携带子目录穿透）
            String safeFileName = safeKey.substring(safeKey.lastIndexOf('/') + 1);
            if (!StringUtils.hasText(safeFileName)) {
                return null;
            }
            Path filePath = Paths.get(UPLOAD_DIR, safeBucket, safeFileName).normalize();
            Path basePath = Paths.get(UPLOAD_DIR, safeBucket).normalize();
            // 规范化后仍需校验路径必须落在 basePath 下
            if (!filePath.startsWith(basePath)) {
                log.warn("本地文件读取路径越界: bucket={}, objectKey={}", bucket, objectKey);
                return null;
            }
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                return null;
            }
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            log.error("本地文件读取失败: bucket={}, objectKey={}: {}", bucket, objectKey, e.getMessage());
            return null;
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
        String name = filename.replaceAll(".*[/\\\\]", "");
        name = name.replaceAll("[\\x00-\\x1F\\x7F]", "");
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
     * 保存文件到本地
     */
    private String saveFile(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }

        try {
            // 确保上传目录存在
            Path uploadPath = Paths.get(UPLOAD_DIR + subDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件：使用 Files.copy 替代 transferTo，
            // 避免 Spring StandardMultipartFile.transferTo 把相对路径解析到 Tomcat work 目录
            Path filePath = uploadPath.resolve(newFilename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("文件上传成功: {}", filePath);

            // 返回访问URL
            return "/api/uploads" + subDir + "/" + newFilename;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件上传失败");
        }
    }
}
