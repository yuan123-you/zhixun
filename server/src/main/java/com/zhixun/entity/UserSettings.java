package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户设置实体，对应 user_settings 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_settings")
public class UserSettings {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID，对应 user_id */
    private Long userId;

    /** 系统通知，对应 notify_system */
    private Integer notifySystem;

    /** 互动通知，对应 notify_interact */
    private Integer notifyInteract;

    /** 私信通知，对应 notify_message */
    private Integer notifyMessage;

    /** 关注通知，对应 notify_follow */
    private Integer notifyFollow;

    /** 显示在线状态，对应 show_online_status */
    private Integer showOnlineStatus;

    /** 私信权限，对应 message_permission */
    private Integer messagePermission;

    /** 保存浏览历史，对应 save_view_history */
    private Integer saveViewHistory;

    /** 字体大小，对应 font_size */
    private Integer fontSize;

    /** 主题，对应 theme */
    private String theme;

    /** 语言，对应 language */
    private String language;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
