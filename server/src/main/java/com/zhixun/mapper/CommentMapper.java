package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 评论 Mapper 接口
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    @Select("SELECT * FROM cms_comment WHERE id=#{commentId} AND deleted_at IS NULL FOR UPDATE")
    Comment selectForUpdate(@Param("commentId") Long commentId);

    @Update("UPDATE cms_comment SET like_count=#{count} WHERE id=#{commentId} AND deleted_at IS NULL")
    int saveLikeCount(@Param("commentId") Long commentId, @Param("count") long count);
}
