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

    /** 文章ID */
    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    /** 父评论ID */
    private Long parentId;

    /** 回复目标用户ID */
    private Long replyUserId;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容最长500个字符")
    private String content;
}
