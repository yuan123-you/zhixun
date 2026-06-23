package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.UserTagFollow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户关注标签 Mapper
 */
@Mapper
public interface UserTagFollowMapper extends BaseMapper<UserTagFollow> {
}
