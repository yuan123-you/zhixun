package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.Collect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 收藏 Mapper 接口
 */
@Mapper
public interface CollectMapper extends BaseMapper<Collect> {
    @Select("SELECT * FROM cms_collect WHERE user_id=#{userId} AND article_id=#{articleId} FOR UPDATE")
    Collect selectInteractionForUpdate(@Param("userId") Long userId, @Param("articleId") Long articleId);

    // Inner CURRENT locking read; NO_MERGE preserves its query block on MySQL 8.
    @Select("SELECT /*+ NO_MERGE(locked_facts) */ COUNT(*) FROM " +
            "(SELECT id FROM cms_collect WHERE article_id=#{articleId} FOR UPDATE) AS locked_facts")
    long countTargetFactsForUpdate(@Param("articleId") Long articleId);
}
