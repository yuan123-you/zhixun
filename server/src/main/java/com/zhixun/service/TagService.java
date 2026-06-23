package com.zhixun.service;

import com.zhixun.entity.Tag;
import com.zhixun.vo.TagVO;

import java.util.List;

/**
 * 标签服务接口
 */
public interface TagService {

    /**
     * 创建标签
     *
     * @param name 标签名称
     * @return 标签ID
     */
    Long create(String name);

    /**
     * 更新标签
     *
     * @param id   标签ID
     * @param name 标签名称
     */
    void update(Long id, String name);

    /**
     * 删除标签
     *
     * @param id 标签ID
     */
    void delete(Long id);

    /**
     * 获取标签详情
     *
     * @param id 标签ID
     * @return 标签实体
     */
    Tag getById(Long id);

    /**
     * 获取标签列表
     *
     * @return 标签列表
     */
    List<TagVO> list();

    /**
     * 获取热门标签列表
     *
     * @param limit 数量
     * @return 热门标签列表
     */
    List<TagVO> hot(int limit);

    /**
     * 合并标签
     *
     * @param sourceTagId 源标签ID（合并后删除）
     * @param targetTagId 目标标签ID（合并到的目标）
     */
    void mergeTag(Long sourceTagId, Long targetTagId);

    /**
     * 获取标签云（所有标签及其文章数）
     *
     * @return 标签列表
     */
    List<TagVO> getTagCloud();

    /**
     * 关注标签
     *
     * @param userId 用户ID
     * @param tagId  标签ID
     */
    void followTag(Long userId, Long tagId);

    /**
     * 取消关注标签
     *
     * @param userId 用户ID
     * @param tagId  标签ID
     */
    void unfollowTag(Long userId, Long tagId);

    /**
     * 获取用户关注的标签列表
     *
     * @param userId 用户ID
     * @return 标签列表
     */
    List<TagVO> getFollowedTags(Long userId);

    /**
     * 判断用户是否关注了某标签
     *
     * @param userId 用户ID
     * @param tagId  标签ID
     * @return true-已关注
     */
    boolean isTagFollowed(Long userId, Long tagId);

    /**
     * 搜索标签（模糊匹配名称，用于自动补全）
     *
     * @param keyword 关键词
     * @return 标签列表
     */
    List<TagVO> searchTags(String keyword);

    /**
     * 同步标签文章数（从 cms_article_tag 重新计算 article_count）
     *
     * @param tagId 标签ID，为null时同步所有标签
     */
    void syncTagArticleCount(Long tagId);
}
