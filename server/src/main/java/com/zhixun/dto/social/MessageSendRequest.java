package com.zhixun.dto.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 私信发送请求
 */
@Data
public class MessageSendRequest {

    /** 接收者ID */
    @NotNull(message = "接收者不能为空")
    private Long receiverId;

    /** 消息内容 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1000, message = "消息内容最长1000个字符")
    private String content;
}
