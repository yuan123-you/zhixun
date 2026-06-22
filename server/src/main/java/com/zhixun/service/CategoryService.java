package com.zhixun.service;

import com.zhixun.entity.Category;
import com.zhixun.vo.CategoryVO;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService {

    /**
     * 创建分类
     *
     * @param name     分类名称
     * @param parentId 父分类ID
     * @param sort     排序值
     * @param icon     图标
     * @return 分类ID
     */
    Long create(String name, Long parentId, Integer sort, String icon);

    /**
     * 更新分类
     *
     * @param id       分类ID
     * @param name     分类名称
     * @param parentId 父分类ID
     * @param sort     排序值
     * @param icon     图标
     */
    void update(Long id, String name, Long parentId, Integer sort, String icon);

    /**
     * 删除分类
     *
     * @param id 分类ID
     */
    void delete(Long id);

    /**
     * 获取分类详情
     *
     * @param id 分类ID
     * @return 分类实体
     */
    Category getById(Long id);

    /**
     * 获取分类列表
     *
     * @return 分类列表
     */
    List<Category> list();

    /**
     * 获取分类树（带 Caffeine 本地缓存）
     *
     * @return 分类树
     */
    List<CategoryVO> tree();
}
