package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告实体，对应 sys_announcement 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_announcement")
public class Announcement implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 类型：1-系统公告，2-活动公告 */
    private Integer type;

    /** 是否置顶：0-否，1-是 */
    private Integer isTop;

    /** 开始展示时间 */
    private LocalDateTime startTime;

    /** 结束展示时间 */
    private LocalDateTime endTime;

    /** 状态：0-禁用，1-启用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
