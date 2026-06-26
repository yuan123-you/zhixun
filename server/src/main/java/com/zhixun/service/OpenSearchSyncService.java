package com.zhixun.service;

import java.util.List;

/**
 * OpenSearch 数据同步服务接口
 */
public interface OpenSearchSyncService {

    /**
     * 同步单篇作品到 OpenSearch
     *
     * @param articleId 作品ID
     */
    void syncArticle(Long articleId);

    /**
     * 批量同步作品到 OpenSearch
     *
     * @param articleIds 作品ID列表
     */
    void syncArticles(List<Long> articleIds);

    /**
     * 从 OpenSearch 删除作品索引
     *
     * @param articleId 作品ID
     */
    void deleteArticle(Long articleId);

    /**
     * 同步单个用户到 OpenSearch
     *
     * @param userId 用户ID
     */
    void syncUser(Long userId);

    /**
     * 批量同步用户到 OpenSearch
     *
     * @param userIds 用户ID列表
     */
    void syncUsers(List<Long> userIds);

    /**
     * 从 OpenSearch 删除用户索引
     *
     * @param userId 用户ID
     */
    void deleteUser(Long userId);

    /**
     * 同步单张图片到 OpenSearch
     *
     * @param imageId 图片ID
     */
    void syncImage(Long imageId);

    /**
     * 批量同步图片到 OpenSearch
     *
     * @param articleId 作品ID（同步该作品下所有图片）
     */
    void syncImagesByArticle(Long articleId);

    /**
     * 从 OpenSearch 删除图片索引
     *
     * @param imageId 图片ID
     */
    void deleteImage(Long imageId);

    /**
     * 删除作品关联的所有图片索引
     *
     * @param articleId 作品ID
     */
    void deleteImagesByArticle(Long articleId);

    /**
     * 全量同步所有数据（作品+用户+图片）
     */
    void fullSync();

    /**
     * 全量同步作品数据
     */
    void fullSyncArticles();

    /**
     * 全量同步用户数据
     */
    void fullSyncUsers();

    /**
     * 全量同步图片数据
     */
    void fullSyncImages();
}
