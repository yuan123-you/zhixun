package com.zhixun.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 评论审核请求
 */
@Data
public class CommentAuditRequest {

    /** 审核动作：approve-通过，reject-拒绝 */
    @NotBlank(message = "审核动作不能为空")
    @Pattern(regexp = "approve|reject", message = "审核动作只能为approve或reject")
    private String action;

    /** 审核意见/拒绝原因 */
    private String reason;
}
