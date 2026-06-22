package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知消息 Mapper 接口
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
