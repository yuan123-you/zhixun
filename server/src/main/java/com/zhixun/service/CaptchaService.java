package com.zhixun.service;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final StringRedisTemplate stringRedisTemplate;
    private final EmailService emailService;

    /** 验证码 Redis Key 前缀 */
    private static final String CAPTCHA_PREFIX = "auth:captcha:";

    /** 验证码发送间隔限制 Key 前缀 */
    private static final String CAPTCHA_INTERVAL_PREFIX = "auth:captcha:interval:";

    /** 验证码长度 */
    private static final int CODE_LENGTH = 6;

    /** 验证码有效期（分钟） */
    private static final long CODE_EXPIRE_MINUTES = 5;

    /** 发送间隔（秒） */
    private static final long SEND_INTERVAL_SECONDS = 60;

    /** 每日发送上限 */
    private static final int DAILY_LIMIT = 10;

    /** 每日发送次数 Key 前缀 */
    private static final String CAPTCHA_DAILY_PREFIX = "auth:captcha:daily:";

    private final SecureRandom random = new SecureRandom();

    /**
     * 发送邮箱验证码
     *
     * @param email   邮箱地址
     * @param purpose 用途（注册/登录/重置密码等）
     */
    public void sendEmailCode(String email, String purpose) {
        // 检查发送间隔
        String intervalKey = CAPTCHA_INTERVAL_PREFIX + email;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(intervalKey))) {
            throw new BusinessException(ErrorCode.OPERATION_TOO_FREQUENT, "发送过于频繁，请60秒后重试");
        }

        // 检查每日发送上限
        String dailyKey = CAPTCHA_DAILY_PREFIX + email;
        String dailyCountStr = stringRedisTemplate.opsForValue().get(dailyKey);
        int dailyCount = dailyCountStr != null ? Integer.parseInt(dailyCountStr) : 0;
        if (dailyCount >= DAILY_LIMIT) {
            throw new BusinessException(ErrorCode.OPERATION_TOO_FREQUENT, "今日验证码发送次数已达上限");
        }

        // 生成6位数字验证码
        String code = generateCode();

        // 存入 Redis
        String codeKey = CAPTCHA_PREFIX + email;
        stringRedisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 设置发送间隔
        stringRedisTemplate.opsForValue().set(intervalKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 递增每日发送次数（当天有效）
        if (dailyCount == 0) {
            stringRedisTemplate.opsForValue().set(dailyKey, "1", 1, TimeUnit.DAYS);
        } else {
            stringRedisTemplate.opsForValue().increment(dailyKey);
        }

        // 异步发送邮件
        emailService.sendVerifyCode(email, code, purpose);

        log.info("验证码已发送: email={}, purpose={}", email, purpose);
    }

    /**
     * 校验验证码
     *
     * @param target 邮箱地址
     * @param code   验证码
     * @return 是否校验通过
     */
    public boolean verifyCode(String target, String code) {
        String codeKey = CAPTCHA_PREFIX + target;
        String storedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(code)) {
            return false;
        }
        // 验证通过后删除验证码，防止重复使用
        stringRedisTemplate.delete(codeKey);
        return true;
    }

    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
