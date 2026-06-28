package com.zhixun.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评论创建请求
 */
@Data
public class CommentCreateRequest {

    /** 作品ID（由Controller从路径参数自动设置） */
    private Long articleId;

    /** 父评论ID */
    private Long parentId;

    /** 回复目标用户ID */
    private Long replyUserId;

    /** 评论内容（与 images 至少有一项非空） */
    @Size(max = 500, message = "评论内容最长500个字符")
    private String content;

    /** 评论图片URL列表（最多9张） */
    @Size(max = 9, message = "评论图片最多9张")
    private java.util.List<String> images;
}
