package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.ArticleLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 点赞 Mapper 接口
 */
@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {
    @Select("SELECT * FROM cms_like WHERE user_id=#{userId} AND target_id=#{targetId} AND target_type=#{targetType} FOR UPDATE")
    ArticleLike selectInteractionForUpdate(@Param("userId") Long userId, @Param("targetId") Long targetId, @Param("targetType") Integer targetType);

    // Lock the inner fact read (CURRENT under InnoDB RR); return only one scalar to Java.
    // NO_MERGE keeps the locking query block separate from the outer aggregation on MySQL 8.
    @Select("SELECT /*+ NO_MERGE(locked_facts) */ COUNT(*) FROM " +
            "(SELECT id FROM cms_like WHERE target_id=#{targetId} AND target_type=#{targetType} FOR UPDATE) AS locked_facts")
    long countTargetFactsForUpdate(@Param("targetId") Long targetId, @Param("targetType") Integer targetType);
}
