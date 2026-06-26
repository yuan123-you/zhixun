package com.zhixun.dto.article;

import lombok.Data;

/**
 * 作品列表查询请求
 */
@Data
public class ArticleQueryRequest {

    /** 关键词搜索 */
    private String keyword;

    /** 分类ID */
    private Long categoryId;

    /** 标签ID */
    private Long tagId;

    /** 作者ID */
    private Long userId;

    /** 作品状态 */
    private Integer status;

    /** 排序字段 */
    private String sortBy;

    /** 排序方向：asc/desc */
    private String sortOrder;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 10;
}
