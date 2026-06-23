package com.zhixun.service;

import com.zhixun.entity.Announcement;
import com.zhixun.vo.AnnouncementVO;

import java.util.List;

/**
 * 公告服务接口
 */
public interface AnnouncementService {

    /**
     * 创建公告
     *
     * @param title     标题
     * @param content   内容
     * @param type      类型
     * @param isTop     是否置顶
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param status    状态
     * @return 公告ID
     */
    Long create(String title, String content, Integer type, Integer isTop,
                java.time.LocalDateTime startTime, java.time.LocalDateTime endTime, Integer status);

    /**
     * 更新公告
     *
     * @param id        公告ID
     * @param title     标题
     * @param content   内容
     * @param type      类型
     * @param isTop     是否置顶
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param status    状态
     */
    void update(Long id, String title, String content, Integer type, Integer isTop,
                java.time.LocalDateTime startTime, java.time.LocalDateTime endTime, Integer status);

    /**
     * 删除公告
     *
     * @param id 公告ID
     */
    void delete(Long id);

    /**
     * 获取管理端公告列表（全部）
     *
     * @return 公告列表
     */
    List<AnnouncementVO> adminList();

    /**
     * 获取有效公告列表（公开，当前时间在展示范围内且启用）
     *
     * @return 公告列表
     */
    List<AnnouncementVO> activeList();
}
