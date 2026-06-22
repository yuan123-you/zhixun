package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.UserMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 私信 Mapper 接口
 */
@Mapper
public interface UserMessageMapper extends BaseMapper<UserMessage> {
}
