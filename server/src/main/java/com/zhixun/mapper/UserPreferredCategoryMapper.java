package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.UserPreferredCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户偏好分类关联 Mapper 接口
 */
@Mapper
public interface UserPreferredCategoryMapper extends BaseMapper<UserPreferredCategory> {
}
