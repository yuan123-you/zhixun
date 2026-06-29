package com.zhixun.vo;

import lombok.Data;

/**
 * 作品互动用户视图（浏览者/点赞者）
 */
@Data
public class ArticleInteractionUserVO {

    /** 用户ID */
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 是否互相关注 */
    private Boolean isMutualFollow;
}
