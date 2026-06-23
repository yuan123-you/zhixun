package com.zhixun.service.impl;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.service.FileService;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地文件存储实现（当MinIO不可用时使用）
 */
@Slf4j
@Service
@ConditionalOnMissingBean(MinioClient.class)
public class LocalFileServiceImpl implements FileService {

    private static final String UPLOAD_DIR = "./uploads";
    private static final String IMAGE_DIR = "/images";
    private static final String FILE_DIR = "/files";

    @Override
    public String uploadImage(MultipartFile file) {
        return saveFile(file, IMAGE_DIR);
    }

    @Override
    public String uploadFile(MultipartFile file) {
        return saveFile(file, FILE_DIR);
    }

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
