package com.zhixun.vo;

import lombok.Data;

import java.util.List;

/**
 * 搜索结果视图
 */
@Data
public class SearchResultVO {

    /** 搜索关键词 */
    private String keyword;

    /** 总结果数 */
    private Long total;

    /** 搜索类型 */
    private String type;

    /** 作品列表（type=article 或 type=all 时有值） */
    private List<ArticleVO> articles;

    /** 作品总数 */
    private Long articleTotal;

    /** 用户列表（type=user 或 type=all 时有值） */
    private List<UserVO> users;

    /** 用户总数 */
    private Long userTotal;

    /** 图片列表（type=image 或 type=all 时有值） */
    private List<ArticleVO> images;

    /** 图片总数 */
    private Long imageTotal;

    /** 搜索耗时（毫秒） */
    private Long took;
}
