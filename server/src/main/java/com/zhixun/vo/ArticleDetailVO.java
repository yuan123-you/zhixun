package com.zhixun.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 作品详情视图（完整内容）
 */
@Data
public class ArticleDetailVO {

    /** 作品ID */
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

    /** 是否置顶 */
    private Integer isTop;

    /** 可见性：0=公开，1=仅粉丝，2=互相关注，3=仅自己 */
    private Integer visibility;

    /** 拒绝原因 */
    private String rejectReason;

    /** 作者信息 */
    private AuthorVO author;

    /** 分类ID */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 标签列表 */
    private List<TagVO> tags;

    /** 当前用户是否已点赞 */
    private Boolean isLiked;

    /** 当前用户是否已收藏 */
    private Boolean isCollected;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 发布设备信息 */
    private String deviceInfo;

    /** 发布位置 */
    private String location;

    /** 发布IP属地 */
    private String ipAddress;

    /**
     * 作者信息嵌套视图
     */
    @Data
    public static class AuthorVO {
        private Long id;
        private String nickname;
        private String avatar;
        private Boolean isFollowing;
    }
}
