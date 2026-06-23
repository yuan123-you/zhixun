package com.zhixun.dto.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告创建/更新请求
 */
@Data
public class AnnouncementRequest {

    /** 标题 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长200个字符")
    private String title;

    /** 内容 */
    @NotBlank(message = "内容不能为空")
    @Size(max = 2000, message = "内容最长2000个字符")
    private String content;

    /** 类型：1-系统公告，2-活动公告 */
    private Integer type = 1;

    /** 是否置顶：0-否，1-是 */
    private Integer isTop = 0;

    /** 开始展示时间 */
    private LocalDateTime startTime;

    /** 结束展示时间 */
    private LocalDateTime endTime;

    /** 状态：0-禁用，1-启用 */
    private Integer status = 1;
}
