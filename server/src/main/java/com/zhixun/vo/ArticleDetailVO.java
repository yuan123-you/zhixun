package com.zhixun.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章详情视图（完整内容）
 */
@Data
public class ArticleDetailVO {

    /** 文章ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 内容 */
    private String content;

    /** 封面图 */
    private String coverImage;

    /** 状态 */
    private Integer status;

    /** 浏览量 */
    private Long viewCount;

    /** 点赞数 */
    private Long likeCount;

    /** 评论数 */
    private Long commentCount;

    /** 收藏数 */
    private Long collectCount;

    /** 分享数 */
    private Long shareCount;

    /** 拒绝原因 */
    private String rejectReason;

    /** 作者昵称 */
    private String authorName;

    /** 作者头像 */
    private String authorAvatar;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 标签列表 */
    private List<TagVO> tags;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 发布设备信息 */
    private String deviceInfo;
}
