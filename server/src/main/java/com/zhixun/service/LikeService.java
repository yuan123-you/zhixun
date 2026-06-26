package com.zhixun.service;

import com.zhixun.common.result.PageResult;
import com.zhixun.vo.ArticleVO;

import java.util.Map;

/**
 * 点赞服务接口
 */
public interface LikeService {

    /**
     * 点赞/取消点赞（切换逻辑）
     *
     * @param userId     用户ID
     * @param targetId   目标ID
     * @param targetType 目标类型（1-作品，2-评论）
     * @return 包含 liked 和 like_count 的结果
     */
    Map<String, Object> toggleLike(Long userId, Long targetId, Integer targetType);

    /**
     * 检查是否已点赞
     *
     * @param userId     用户ID
     * @param targetId   目标ID
     * @param targetType 目标类型
     * @return true-已点赞
     */
    boolean isLiked(Long userId, Long targetId, Integer targetType);

    /**
     * 获取我的点赞列表
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 点赞的作品分页列表
     */
    PageResult<ArticleVO> getUserLikes(Long userId, Integer page, Integer pageSize);
}
