package com.zhixun.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupVO {
    private Long id;
    private String name;
    private String groupNumber;
    private String avatar;
    private String description;
    private Long ownerId;
    private String ownerName;
    private Long memberCount;
    private Integer maxMembers;
    private Integer isPublic;
    /** 群组状态：0=正常，1=禁言，2=已解散 */
    private Integer status;
    private Integer myRole;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}