package com.zhixun.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.common.util.AesUtil;
import com.zhixun.entity.User;
import com.zhixun.enums.RoleEnum;
import com.zhixun.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 默认用户数据初始化
 * 确保超级管理员账号存在，账号与密码仅通过环境变量显式配置，未配置时不创建账号
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class DefaultUserDataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AesUtil aesUtil;

    @Override
    public void run(String... args) {
        initializeAdmin(System.getenv("ADMIN_USERNAME"), System.getenv("ADMIN_PASSWORD"));
    }

    void initializeAdmin(String adminUsername, String adminPassword) {
        if (adminPassword == null || adminPassword.isBlank()
                || adminUsername == null || adminUsername.isBlank()) {
            log.warn("Administrator initialization skipped: explicit credentials are required");
            return;
        }

        // 检查管理员是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, adminUsername);
        User existingAdmin = userMapper.selectOne(wrapper);

        if (existingAdmin != null) {
            if ((existingAdmin.getRole() == RoleEnum.SUPER_ADMIN || existingAdmin.getRole() == RoleEnum.ADMIN)
                    && (existingAdmin.getPasswordHash() == null || existingAdmin.getPasswordHash().isBlank())) {
                User update = new User();
                update.setId(existingAdmin.getId());
                update.setPasswordHash(passwordEncoder.encode(adminPassword));
                userMapper.updateById(update);
            }
            // 如果已存在但缺少UID，补充默认UID
            if (existingAdmin.getUid() == null) {
                User updateUser = new User();
                updateUser.setId(existingAdmin.getId());
                updateUser.setUid(generateDefaultUid());
                userMapper.updateById(updateUser);
                log.info("已为管理员账号 {} 补充UID", adminUsername);
            }
            log.info("管理员账号已存在，跳过初始化");
            return;
        }

        // 创建默认超级管理员
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setNickname("知讯管理员");
        admin.setEmail(aesUtil.encrypt("admin@zhixun.com"));
        admin.setUid(generateDefaultUid());
        admin.setRole(RoleEnum.SUPER_ADMIN);
        admin.setStatus(User.STATUS_NORMAL);
        admin.setBio("知讯平台超级管理员");
        admin.setIsOnline(0);

        userMapper.insert(admin);
        log.info("管理员账号初始化完成");
    }

    /**
     * 生成默认UID（格式: user_ + 8位随机字符）
     */
    private String generateDefaultUid() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("user_");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

}
