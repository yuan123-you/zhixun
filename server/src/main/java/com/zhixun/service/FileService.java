package com.zhixun.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

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
     * @return 文件访问URL（服务器相对路径，通过 /api/v1/files/view/** 访问）
     */
    String uploadImage(MultipartFile file);

    /**
     * 上传附件
     * - 大小限制 20MB
     *
     * @param file 附件文件
     * @return 文件访问URL（服务器相对路径）
     */
    String uploadFile(MultipartFile file) throws Exception;

    /**
     * 上传语音
     * - 支持 webm/wav/mp3/m4a/ogg 格式
     * - 大小限制 10MB
     * - 上传到 MinIO
     *
     * @param file 语音文件
     * @return 文件访问URL（服务器相对路径）
     */
    String uploadVoice(MultipartFile file) throws Exception;

    /**
     * 从 MinIO 读取文件流
     * 用于在浏览器端访问时由后端代理读取，避免 MinIO 重定向/ORB 问题
     *
     * @param bucket    存储桶名
     * @param objectKey 对象 key（不含 bucket 的路径）
     * @return 文件输入流；不存在时返回 null
     */
    InputStream readFile(String bucket, String objectKey);
}
