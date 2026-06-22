package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.UserSettings;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户偏好设置 Mapper 接口
 */
@Mapper
public interface UserSettingsMapper extends BaseMapper<UserSettings> {
}
