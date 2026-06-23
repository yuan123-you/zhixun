package com.zhixun.dto.banner;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 轮播图创建/更新请求
 */
@Data
public class BannerRequest {

    /** 标题 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长100个字符")
    private String title;

    /** 图片地址 */
    @NotBlank(message = "图片地址不能为空")
    private String imageUrl;

    /** 链接地址 */
    private String linkUrl;

    /** 链接类型：1-文章，2-外链 */
    private Integer linkType = 1;

    /** 排序值 */
    private Integer sortOrder = 0;

    /** 开始展示时间 */
    private LocalDateTime startTime;

    /** 结束展示时间 */
    private LocalDateTime endTime;

    /** 状态：0-禁用，1-启用 */
    private Integer status = 1;
}
