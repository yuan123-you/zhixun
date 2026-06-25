package com.zhixun.dto.article;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 文章更新请求
 */
@Data
public class ArticleUpdateRequest {

    /** 标题 */
    @Size(max = 100, message = "标题最长100个字符")
    private String title;

    /** 分类ID */
    private Long categoryId;

    /** 标签ID列表 */
    private List<Long> tagIds;

    /** 摘要 */
    @Size(max = 500, message = "摘要最长500个字符")
    private String summary;

    /** 内容 */
    private String content;

    /** 封面图 */
    private String coverImage;

    /** 文章状态：0-草稿，1-待审核，2-已发布 */
    private Integer status;

    /** 可见性：0=公开，1=仅粉丝，2=互相关注，3=仅自己 */
    private Integer visibility;

    /** 发布位置 */
    private String location;
}
