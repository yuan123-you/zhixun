package com.zhixun.controller;

import com.zhixun.common.result.R;
import com.zhixun.service.FileService;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 */
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
    @SentinelResource(value = "file-upload", blockHandler = "uploadBlockHandler", blockHandlerClass = FileController.BlockHandlers.class)
    public R<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return R.ok(fileService.uploadImage(file));
    }

    /**
     * 上传附件
     */
    @PostMapping("/upload/file")
    @PreAuthorize("isAuthenticated()")
    @SentinelResource(value = "file-upload", blockHandler = "uploadBlockHandler", blockHandlerClass = FileController.BlockHandlers.class)
    public R<String> uploadFile(@RequestParam("file") MultipartFile file) {
        return R.ok(fileService.uploadFile(file));
    }

    /**
     * 上传语音
     */
    @PostMapping("/upload/voice")
    @PreAuthorize("isAuthenticated()")
    @SentinelResource(value = "file-upload", blockHandler = "uploadBlockHandler", blockHandlerClass = FileController.BlockHandlers.class)
    public R<String> uploadVoice(@RequestParam("file") MultipartFile file) {
        return R.ok(fileService.uploadVoice(file));
    }

    /**
     * Sentinel 限流降级处理
     */
    public static class BlockHandlers {
        public static R<String> uploadBlockHandler(MultipartFile file, BlockException e) {
            return R.fail(429, "上传请求过于频繁，请稍后重试");
        }
    }
}
