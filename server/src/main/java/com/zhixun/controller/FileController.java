package com.zhixun.controller;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.R;
import com.zhixun.service.FileService;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传图片
     */
    @PostMapping("/upload/image")
    @PreAuthorize("isAuthenticated()")
    @SentinelResource(value = "file-upload",
            blockHandler = "uploadBlockHandler", blockHandlerClass = FileController.BlockHandlers.class,
            fallback = "uploadFallback", fallbackClass = FileController.BlockHandlers.class,
            exceptionsToIgnore = { BusinessException.class })
    public R<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return R.ok(fileService.uploadImage(file));
    }

    /**
     * 上传附件
     */
    @PostMapping("/upload/file")
    @PreAuthorize("isAuthenticated()")
    @SentinelResource(value = "file-upload",
            blockHandler = "uploadBlockHandler", blockHandlerClass = FileController.BlockHandlers.class,
            fallback = "uploadFallback", fallbackClass = FileController.BlockHandlers.class,
            exceptionsToIgnore = { BusinessException.class })
    public R<String> uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
        return R.ok(fileService.uploadFile(file));
    }

    /**
     * 上传语音
     */
    @PostMapping("/upload/voice")
    @PreAuthorize("isAuthenticated()")
    @SentinelResource(value = "file-upload",
            blockHandler = "uploadBlockHandler", blockHandlerClass = FileController.BlockHandlers.class,
            fallback = "uploadFallback", fallbackClass = FileController.BlockHandlers.class,
            exceptionsToIgnore = { BusinessException.class })
    public R<String> uploadVoice(@RequestParam("file") MultipartFile file) throws Exception {
        return R.ok(fileService.uploadVoice(file));
    }

    /**
     * 查看/下载文件（由后端从 MinIO 读取并流式返回）
     *
     * 用于解决以下问题：
     * 1. MinIO 在对象无签名时返回 307 重定向到控制台（9001）触发 ORB
     * 2. MinIO 签名 URL 与 Vite 代理交互时的 ERR_TOO_MANY_REDIRECTS
     * 3. 跨域/防盗链场景下直接访问 MinIO 不便
     *
     * 路径格式：/api/v1/files/view/{bucket}/{objectKey}
     * 例如：/api/v1/files/view/zhixun/images/2026/06/28/abc.jpg
     */
    @GetMapping("/view/{bucket}/**")
    public void viewFile(@PathVariable("bucket") String bucket,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        // 提取 objectKey：从请求 URI 中移除 /v1/files/view/{bucket}/ 前缀
        // 注意：context-path=/api 已被 Spring 剥离，所以这里只需要处理 /v1/files/view/...
        String requestUri = request.getRequestURI();
        String prefix = "/v1/files/view/" + bucket + "/";
        String objectKey = "";
        if (requestUri.startsWith(prefix)) {
            objectKey = requestUri.substring(prefix.length());
        } else {
            // 兜底：尝试移除 /api 前缀
            int idx = requestUri.indexOf("/view/" + bucket + "/");
            if (idx >= 0) {
                objectKey = requestUri.substring(idx + ("/view/" + bucket + "/").length());
            }
        }

        if (!StringUtils.hasText(objectKey) || objectKey.contains("..")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法的文件路径");
        }

        log.debug("代理查看文件: bucket={}, objectKey={}", bucket, objectKey);

        // 优先从 MinIO 读取
        InputStream inputStream = fileService.readFile(bucket, objectKey);

        // 降级方案：MinIO 不可用时，从本地存储读取
        if (inputStream == null) {
            inputStream = readFileFromLocal(bucket, objectKey);
        }

        if (inputStream == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (InputStream in = inputStream) {
            // 根据 objectKey 推断 Content-Type
            String contentType = guessContentType(objectKey);
            response.setContentType(contentType);
            response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");
            // 启用跨域（图片通常由 img 标签加载，跨域读不需要 CORS，但加 CORS 头便于 fetch/canvas）
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            // 可在线预览的文件类型设置 Content-Disposition: inline，允许浏览器直接展示
            // 其他类型设置 attachment 触发下载
            if (contentType.startsWith("image/") || contentType.equals("application/pdf")
                    || contentType.startsWith("text/") || contentType.startsWith("audio/")) {
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline");
            } else {
                String fileName = objectKey.contains("/") ? objectKey.substring(objectKey.lastIndexOf('/') + 1) : objectKey;
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
            }
            try (OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }
        } catch (Exception e) {
            log.error("文件流式返回失败, bucket={}, objectKey={}: {}", bucket, objectKey, e.getMessage());
            // 响应已写入，无法再设置状态码，记录日志即可
        }
    }

    /**
     * 从本地存储读取文件（MinIO 不可用时的降级方案）
     *
     * 兼容以下几种本地存储布局（按顺序查找，命中即返回）：
     * 1) bucket=zhixun, objectKey=images/2026/06/27/abc.jpg —— MinIO 桶格式
     *    -> 实际存储在 ./uploads/images/2026/06/27/abc.jpg（去 bucket 名前缀，取子目录前段对齐 subDir）
     *    FileServiceImpl 降级写入格式：./uploads/{subDir}/{yyyy/MM/dd}/{uuid}.{ext}
     * 2) bucket=images, objectKey=2026/06/27/abc.jpg —— 简化的子目录格式
     *    -> 实际存储在 ./uploads/images/2026/06/27/abc.jpg
     * 3) bucket=images, objectKey=abc.jpg —— 平铺格式（LocalFileServiceImpl 写入格式）
     *    -> 实际存储在 ./uploads/images/abc.jpg
     * 4) bucket=任意, objectKey=2026/06/27/abc.jpg —— bucket 名前缀
     *    -> 实际存储在 ./uploads/{bucket}/2026/06/27/abc.jpg
     *
     * 同时做路径遍历校验，防止越权访问上传目录之外的文件。
     */
    private InputStream readFileFromLocal(String bucket, String objectKey) {
        try {
            String safeBucket = bucket.replaceAll(".*[/\\\\]", "").trim();
            String safeKey = objectKey.replace("\\", "/");
            if (safeKey.contains("..")) return null;
            // 去掉开头斜杠
            if (safeKey.startsWith("/")) {
                safeKey = safeKey.substring(1);
            }
            if (!StringUtils.hasText(safeKey)) return null;

            // 取文件名（防止 objectKey 携带子目录穿透）
            String safeFileName = safeKey.substring(safeKey.lastIndexOf('/') + 1);
            if (!StringUtils.hasText(safeFileName)) return null;

            Path basePath = Paths.get(LOCAL_UPLOAD_BASE).toAbsolutePath().normalize();

            // 1) MinIO 桶名（zhixun）作为 bucket 时的处理：去除 bucket 名前缀后，
            //    把 key 的第一段视作 subDir（"images"/"files"/"voices"）。
            //    例：bucket=zhixun, key=images/2026/06/27/abc.jpg
            //       -> ./uploads/images/2026/06/27/abc.jpg
            String[] knownSubDirs = {"images", "files", "voices"};
            java.util.List<String> candidates = new java.util.ArrayList<>();
            for (String sub : knownSubDirs) {
                if (safeKey.startsWith(sub + "/")) {
                    String rest = safeKey.substring(sub.length() + 1);
                    candidates.add("./" + LOCAL_UPLOAD_BASE + "/" + sub + "/" + rest);
                }
            }
            // 2) 直接以 bucket 作为子目录：./uploads/{bucket}/{key}
            candidates.add("./" + LOCAL_UPLOAD_BASE + "/" + safeBucket + "/" + safeKey);
            // 3) 去除 bucket 名前缀后：./uploads/{key}（如 ./uploads/images/2026/06/27/abc.jpg）
            candidates.add("./" + LOCAL_UPLOAD_BASE + "/" + safeKey);
            // 4) 平铺格式：./uploads/{bucket}/{filename}
            candidates.add("./" + LOCAL_UPLOAD_BASE + "/" + safeBucket + "/" + safeFileName);
            // 5) 平铺格式：./uploads/{filename}（子目录丢失情形）
            candidates.add("./" + LOCAL_UPLOAD_BASE + "/" + safeFileName);

            for (String p : candidates) {
                java.nio.file.Path path = java.nio.file.Paths.get(p).normalize();
                // 必须在 basePath 之下
                if (!path.toAbsolutePath().startsWith(basePath)) {
                    continue;
                }
                if (java.nio.file.Files.exists(path) && !java.nio.file.Files.isDirectory(path)) {
                    log.debug("从本地存储读取文件: {}", path);
                    return java.nio.file.Files.newInputStream(path);
                }
            }
            log.debug("本地存储未找到文件: bucket={}, objectKey={}, candidates={}", bucket, objectKey, candidates);
            return null;
        } catch (Exception e) {
            log.debug("本地存储读取失败: bucket={}, objectKey={}: {}", bucket, objectKey, e.getMessage());
            return null;
        }
    }

    /** 本地上传根目录（与 FileServiceImpl.LOCAL_UPLOAD_DIR 保持一致） */
    private static final String LOCAL_UPLOAD_BASE = "uploads";

    /**
     * 根据文件名后缀推断 Content-Type
     */
    private String guessContentType(String filename) {
        if (filename == null) return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        if (lower.endsWith(".webm")) return "audio/webm";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (lower.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        if (lower.endsWith(".txt")) return "text/plain; charset=utf-8";
        if (lower.endsWith(".zip")) return "application/zip";
        if (lower.endsWith(".rar")) return "application/vnd.rar";
        if (lower.endsWith(".7z")) return "application/x-7z-compressed";
        if (lower.endsWith(".tar")) return "application/x-tar";
        if (lower.endsWith(".gz")) return "application/gzip";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    /**
     * Sentinel 限流降级处理
     */
    public static class BlockHandlers {
        private static final org.slf4j.Logger blockLog = org.slf4j.LoggerFactory.getLogger("FileController.BlockHandlers");

        /** 限流/熔断拦截处理 */
        public static R<String> uploadBlockHandler(MultipartFile file, BlockException e) {
            return R.fail(429, "上传请求过于频繁，请稍后重试");
        }

        /** 业务异常兜底降级（非限流类异常，如 AOP 代理异常、参数解析异常等） */
        public static R<String> uploadFallback(MultipartFile file, Throwable e) {
            blockLog.error("文件上传降级处理: fileName={}, error={}",
                    file != null ? file.getOriginalFilename() : "null",
                    e != null ? e.getMessage() : "unknown", e);
            return R.fail(ErrorCode.FILE_UPLOAD_FAILED, "文件上传服务暂时不可用，请稍后重试");
        }
    }
}
