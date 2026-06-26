package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_topic")
public class Topic implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String coverImage;
    private Long articleCount = 0L;
    private Long followCount = 0L;
    private BigDecimal hotScore;
    private Integer isOfficial = 0;
    private Integer status = 0;
    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Boolean isFollowed;

    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_HIDDEN = 1;
}
