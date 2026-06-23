package com.zhixun.service;

import com.zhixun.entity.Banner;
import com.zhixun.vo.BannerVO;

import java.util.List;

/**
 * 轮播图服务接口
 */
public interface BannerService {

    /**
     * 创建轮播图
     *
     * @param title     标题
     * @param imageUrl  图片地址
     * @param linkUrl   链接地址
     * @param linkType  链接类型
     * @param sortOrder 排序值
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param status    状态
     * @return 轮播图ID
     */
    Long create(String title, String imageUrl, String linkUrl, Integer linkType,
                Integer sortOrder, java.time.LocalDateTime startTime,
                java.time.LocalDateTime endTime, Integer status);

    /**
     * 更新轮播图
     *
     * @param id        轮播图ID
     * @param title     标题
     * @param imageUrl  图片地址
     * @param linkUrl   链接地址
     * @param linkType  链接类型
     * @param sortOrder 排序值
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param status    状态
     */
    void update(Long id, String title, String imageUrl, String linkUrl, Integer linkType,
                Integer sortOrder, java.time.LocalDateTime startTime,
                java.time.LocalDateTime endTime, Integer status);

    /**
     * 删除轮播图
     *
     * @param id 轮播图ID
     */
    void delete(Long id);

    /**
     * 获取管理端轮播图列表（全部）
     *
     * @return 轮播图列表
     */
    List<BannerVO> adminList();

    /**
     * 获取有效轮播图列表（公开，当前时间在展示范围内且启用）
     *
     * @return 轮播图列表
     */
    List<BannerVO> activeList();
}
