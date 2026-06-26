package com.zhixun.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签视图
 */
@Data
public class TagVO {

    /** 标签ID */
    private Long id;

    /** 标签名称 */
    private String name;

    /** 关联作品数 */
    private Long articleCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 当前用户是否已关注 */
    private Boolean isFollowed;
}
