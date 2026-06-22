package com.zhixun.service.impl;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务Fallback实现（当MinIO不可用时使用本地存储）
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "minio.enabled", havingValue = "false", matchIfMissing = false)
public class LocalFileServiceImpl implements FileService {

    @Override
    public String uploadImage(MultipartFile file) {
        throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件存储服务暂不可用");
    }

    @Override
    public String uploadFile(MultipartFile file) {
        throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件存储服务暂不可用");
    }
}
