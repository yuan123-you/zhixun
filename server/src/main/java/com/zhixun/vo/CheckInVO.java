package com.zhixun.vo;

import lombok.Data;

@Data
public class CheckInVO {
    private Boolean hasCheckedIn;
    private Integer consecutiveDays;
    private Integer todayPoints;
    private Long totalExp;
    private Integer level;
    private String levelName;
}