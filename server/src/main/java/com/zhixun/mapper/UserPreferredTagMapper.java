package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.UserPreferredTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户偏好标签关联 Mapper 接口
 */
@Mapper
public interface UserPreferredTagMapper extends BaseMapper<UserPreferredTag> {
}
