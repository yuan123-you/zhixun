package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.GroupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

import java.util.List;

@Mapper
public interface GroupMessageMapper extends BaseMapper<GroupMessage> {
    @Insert("INSERT INTO cms_group_message(group_id,sender_id,content,message_type,created_at) " +
            "SELECT #{message.groupId},0,#{message.content},'ai_reply',#{message.createdAt} " +
            "WHERE EXISTS (SELECT 1 FROM cms_group_info WHERE id=#{message.groupId} AND status=0) " +
            "AND EXISTS (SELECT 1 FROM cms_group_member WHERE group_id=#{message.groupId} AND user_id=#{requesterId})")
    @Options(useGeneratedKeys = true, keyProperty = "message.id", keyColumn = "id")
    int insertAiIfActive(@Param("message") GroupMessage message, @Param("requesterId") Long requesterId);

    List<GroupMessage> selectByGroupId(@Param("groupId") Long groupId, @Param("offset") Long offset, @Param("limit") int limit);
}