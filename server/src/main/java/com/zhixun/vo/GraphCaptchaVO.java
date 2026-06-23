package com.zhixun.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图形验证码响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphCaptchaVO {

    /** 验证码键（用于后续校验） */
    private String captchaKey;

    /** Base64 编码的图片 */
    private String image;
}
