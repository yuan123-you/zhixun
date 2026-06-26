package com.zhixun.service;

import com.zhixun.common.result.PageResult;
import com.zhixun.vo.ArticleVO;

import java.util.List;
import java.util.Map;

/**
 * 收藏服务接口
 */
public interface CollectService {

    /**
     * 收藏/取消收藏（切换逻辑）
     *
     * @param userId    用户ID
     * @param articleId 作品ID
     * @param groupName 收藏夹分组名（可选）
     * @return 包含 collected 和 collect_count 的结果
     */
    Map<String, Object> toggleCollect(Long userId, Long articleId, String groupName);

    /**
     * 获取我的收藏列表
     *
     * @param userId    用户ID
     * @param groupName 收藏夹分组名（可选）
     * @param page      页码
     * @param pageSize  每页大小
     * @return 收藏的作品分页列表
     */
    PageResult<ArticleVO> getUserCollects(Long userId, String groupName, Integer page, Integer pageSize);

    /**
     * 获取收藏夹分组列表
     *
     * @param userId 用户ID
     * @return 分组名称列表
     */
    List<String> getCollectGroups(Long userId);
}
