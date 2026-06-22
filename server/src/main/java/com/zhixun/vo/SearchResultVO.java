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

    /** 文章列表（type=article 或 type=all 时有值） */
    private List<ArticleVO> articles;

    /** 用户列表（type=user 或 type=all 时有值） */
    private List<UserVO> users;

    /** 图片列表（type=image 或 type=all 时有值） */
    private List<ArticleVO> images;

    /** 搜索耗时（毫秒） */
    private Long took;
}
