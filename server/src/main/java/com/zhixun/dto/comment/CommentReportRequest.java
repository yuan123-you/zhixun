package com.zhixun.dto.comment;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评论举报请求
 */
@Data
public class CommentReportRequest {

    /** 举报原因 */
    @Size(max = 500, message = "举报原因最长500个字符")
    private String reason;
}
