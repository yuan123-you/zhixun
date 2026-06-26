package com.zhixun.dto.article;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 作品可见性变更请求
 */
@Data
public class ArticleVisibilityRequest {

    /** 可见性：0=公开，1=仅粉丝，2=互相关注，3=仅自己 */
    @NotNull(message = "可见性不能为空")
    @Min(value = 0, message = "可见性取值范围为 0-3")
    @Max(value = 3, message = "可见性取值范围为 0-3")
    private Integer visibility;
}
