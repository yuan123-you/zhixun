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

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传服务实现
 * 使用 MinIO 对象存储
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true")
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

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

        // 上传文件到 MinIO
        return uploadToMinio(file, filePath, IMAGE_EXTENSIONS.get(extension.toLowerCase()));
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

        // 上传文件到 MinIO
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            contentType = "application/octet-stream";
        }
        return uploadToMinio(file, filePath, contentType);
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

    /**
     * 上传文件到 MinIO
     */
    private String uploadToMinio(MultipartFile file, String filePath, String contentType) {
        try {
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
                            .object(filePath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build());

            // 返回文件访问URL
            return minioConfig.getEndpoint() + "/" + bucketName + "/" + filePath;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件上传失败");
        }
    }
}
