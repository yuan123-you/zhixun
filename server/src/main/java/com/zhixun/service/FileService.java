package com.zhixun.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 */
public interface FileService {

    /**
     * 上传图片
     * - 白名单扩展名校验（jpg/png/gif/webp）
     * - 文件头魔数校验
     * - 文件大小限制（5MB）
     * - 上传到 MinIO
     *
     * @param file 图片文件
     * @return 文件访问URL
     */
    String uploadImage(MultipartFile file);

    /**
     * 上传附件
     * - 大小限制 20MB
     *
     * @param file 附件文件
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file);

    /**
     * 上传语音
     * - 支持 webm/wav/mp3/m4a/ogg 格式
     * - 大小限制 10MB
     * - 上传到 MinIO
     *
     * @param file 语音文件
     * @return 文件访问URL
     */
    String uploadVoice(MultipartFile file);
}
