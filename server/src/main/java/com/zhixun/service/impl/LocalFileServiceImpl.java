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

            // 保存文件
            Path filePath = uploadPath.resolve(newFilename);
            file.transferTo(filePath.toFile());

            log.info("文件上传成功: {}", filePath);

            // 返回访问URL
            return "/api/uploads" + subDir + "/" + newFilename;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件上传失败");
        }
    }
}
