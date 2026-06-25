package com.zhixun.dto.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章创建请求
 */
@Data
public class ArticleCreateRequest {

    /** 标题 */
    @NotBlank(message = "标题不能为空")
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
    @NotBlank(message = "内容不能为空")
    private String content;

    /** 封面图 */
    private String coverImage;

    /** 文章状态：0-草稿，1-待审核 */
    private Integer status;

    /** 定时发布时间 */
    private LocalDateTime publishAt;

    /** 发布设备信息 */
    private String deviceInfo;
}
