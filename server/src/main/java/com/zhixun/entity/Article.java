package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhixun.enums.ArticleStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 文章实体，对应 cms_article 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_article")
public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作者ID，对应 author_id */
    private Long authorId;

    /** 分类ID，对应 category_id */
    private Long categoryId;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 内容 */
    private String content;

    /** 封面图，对应 cover_image */
    private String coverImage;

    /** 文章状态 */
    private ArticleStatusEnum status;

    /** 是否置顶：0-否，1-是，对应 is_top */
    private Integer isTop;

    /** 是否推荐：0-否，1-是，对应 is_recommend */
    private Integer isRecommend;

    /** 浏览量，对应 view_count */
    private Long viewCount;

    /** 点赞数，对应 like_count */
    private Long likeCount;

    /** 评论数，对应 comment_count */
    private Long commentCount;

    /** 收藏数，对应 collect_count */
    private Long collectCount;

    /** 分享数，对应 share_count */
    private Long shareCount;

    /** 热度分，对应 hot_score */
    private BigDecimal hotScore;

    /** 发布时间，对应 publish_at */
    private LocalDateTime publishAt;

    /** 拒绝原因，对应 reject_reason */
    private String rejectReason;

    /** 发布设备信息，对应 device_info */
    private String deviceInfo;

    /** 发布位置，对应 location */
    private String location;

    /** 发布IP属地，对应 ip_address */
    private String ipAddress;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 可见性：0=公开，1=仅粉丝，2=互相关注，3=仅自己，对应 visibility */
    private Integer visibility;

    /** 删除时间，DATETIME类型，NULL=未删除，非NULL=已删除 */
    private LocalDateTime deletedAt;

    /** 状态常量 - 草稿 */
    public static final int STATUS_DRAFT = 0;

    /** 状态常量 - 待审核 */
    public static final int STATUS_PENDING = 1;

    /** 状态常量 - 已发布 */
    public static final int STATUS_PUBLISHED = 2;

    /** 状态常量 - 驳回 */
    public static final int STATUS_REJECTED = 3;

    /** 状态常量 - 下架 */
    public static final int STATUS_OFFLINE = 4;

    /** 可见性常量 - 公开 */
    public static final int VISIBILITY_PUBLIC = 0;

    /** 可见性常量 - 仅粉丝 */
    public static final int VISIBILITY_FOLLOWERS = 1;

    /** 可见性常量 - 仅互关 */
    public static final int VISIBILITY_MUTUAL = 2;

    /** 可见性常量 - 仅自己 */
    public static final int VISIBILITY_PRIVATE = 3;
}
