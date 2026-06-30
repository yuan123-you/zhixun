package com.zhixun.security;

import com.zhixun.entity.User;
import com.zhixun.enums.RoleEnum;
import com.zhixun.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义用户详情服务
 * 从数据库加载用户信息，返回 Spring Security 的 UserDetails
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库查询用户（使用自定义 SQL 查询包含 passwordHash，因为 @TableField(select = false) 会阻止 MyBatis-Plus 查询该字段）
        User user = userMapper.selectByUsernameWithPassword(username);
        if (user == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 检查账号状态
        boolean enabled = user.getStatus() != null && user.getStatus() == User.STATUS_NORMAL;
        if (!enabled) {
            log.warn("账号已被禁用: {}", username);
            throw new UsernameNotFoundException("账号已被禁用");
        }

        // 构建权限列表
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (RoleEnum.ADMIN.equals(user.getRole()) || RoleEnum.SUPER_ADMIN.equals(user.getRole())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        if (RoleEnum.SUPER_ADMIN.equals(user.getRole())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }

        // 返回 Spring Security 的 UserDetails
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(!enabled)
                .credentialsExpired(false)
                .disabled(!enabled)
                .build();
    }
}
