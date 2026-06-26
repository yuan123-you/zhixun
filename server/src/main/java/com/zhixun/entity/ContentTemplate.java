package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_template")
public class ContentTemplate implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String coverImage;
    private String category;
    private String content;
    private String tags;
    private Long useCount = 0L;
    private Integer isOfficial = 0;
    private Long creatorId;
    private Integer status = 0;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableField(exist = false)
    private String creatorName;
    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_HIDDEN = 1;
}