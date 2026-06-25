package com.zhixun.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章列表视图（含作者、分类、标签摘要）
 */
@Data
public class ArticleVO {

    /** 文章ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

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

    /** 是否置顶 */
    private Integer isTop;

    /** 发布设备信息 */
    private String deviceInfo;

    /** 作者昵称 */
    private String authorName;

    /** 作者头像 */
    private String authorAvatar;

    /** 分类名称 */
    private String categoryName;

    /** 标签列表 */
    private List<TagVO> tags;

    /** 搜索结果中的正文内容片段（含高亮标记） */
    private String contentSnippet;

    /** 搜索匹配类型：title=标题匹配, content=正文匹配, summary=摘要匹配 */
    private String matchType;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
