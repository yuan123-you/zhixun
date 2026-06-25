package com.zhixun.dto.article;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 草稿发布请求
 */
@Data
public class ArticlePublishRequest {

    /** 定时发布时间（null 表示立即发布） */
    private LocalDateTime publishAt;
}
