package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户（包含密码哈希，用于登录验证）
     * 需要单独方法是因为 User 实体中 passwordHash 标注了 @TableField(select = false)，
     * MyBatis-Plus 的 LambdaQueryWrapper.select() 无法覆盖该限制
     */
    @Select("SELECT id, uid, username, password_hash, nickname, avatar, email, phone, " +
            "status, role, last_login_at, created_at, updated_at " +
            "FROM sys_user WHERE username = #{username}")
    User selectByUsernameWithPassword(String username);

    /**
     * 根据 ID 查询用户（包含密码哈希，用于修改密码验证）
     */
    @Select("SELECT id, username, password_hash FROM sys_user WHERE id = #{id}")
    User selectByIdWithPassword(Long id);

    /**
     * 根据 UID 查询用户
     */
    @Select("SELECT * FROM sys_user WHERE uid = #{uid}")
    User selectByUid(String uid);
}
