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
}
