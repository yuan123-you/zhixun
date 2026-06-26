package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_check_in")
public class UserCheckIn implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate checkInDate;
    private Integer consecutiveDays = 1;
    private Integer points = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
