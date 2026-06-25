package com.zhixun.service;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    private final GraphCaptchaService graphCaptchaService;

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

    /** 单号每日发送上限 */
    private static final int DAILY_LIMIT_PER_ACCOUNT = 10;

    /** 单 IP 每日发送上限 */
    private static final int DAILY_LIMIT_PER_IP = 30;

    /** 每日发送次数 Key 前缀（按邮箱） */
    private static final String CAPTCHA_DAILY_PREFIX = "auth:captcha:daily:";

    /** 每日发送次数 Key 前缀（按 IP） */
    private static final String CAPTCHA_DAILY_IP_PREFIX = "auth:captcha:daily:ip:";

    private final SecureRandom random = new SecureRandom();

    /**
     * 发送邮箱验证码
     *
     * @param email        邮箱地址
     * @param purpose      用途（注册/登录/重置密码等）
     * @param captchaKey   图形验证码键
     * @param captchaAnswer 图形验证码答案
     */
    public void sendEmailCode(String email, String purpose, String captchaKey, String captchaAnswer) {
        // 0. 校验邮箱地址有效性 & 统一转为小写用于 Redis Key
        if (email == null || email.isBlank() || email.toLowerCase().endsWith("@example.com")
                || email.toLowerCase().endsWith("@example.org") || email.toLowerCase().endsWith("@test.com")
                || !email.contains("@")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱地址无效");
        }
        String normalizedEmail = email.toLowerCase().trim();

        // 1. 检查发送间隔（先检查，避免过早消费图形验证码）
        String intervalKey = CAPTCHA_INTERVAL_PREFIX + normalizedEmail;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(intervalKey))) {
            throw new BusinessException(ErrorCode.OPERATION_TOO_FREQUENT, "发送过于频繁，请60秒后重试");
        }

        // 2. 检查单号每日发送上限
        String dailyKey = CAPTCHA_DAILY_PREFIX + normalizedEmail;
        String dailyCountStr = stringRedisTemplate.opsForValue().get(dailyKey);
        int dailyCount = dailyCountStr != null ? Integer.parseInt(dailyCountStr) : 0;
        if (dailyCount >= DAILY_LIMIT_PER_ACCOUNT) {
            throw new BusinessException(ErrorCode.OPERATION_TOO_FREQUENT, "今日验证码发送次数已达上限");
        }

        // 3. 检查 IP 每日发送上限
        String clientIp = getClientIp();
        int ipCount = 0;
        if (clientIp != null) {
            String dailyIpKey = CAPTCHA_DAILY_IP_PREFIX + clientIp;
            String ipCountStr = stringRedisTemplate.opsForValue().get(dailyIpKey);
            ipCount = ipCountStr != null ? Integer.parseInt(ipCountStr) : 0;
            if (ipCount >= DAILY_LIMIT_PER_IP) {
                log.warn("IP 每日发送上限触发: ip={}, count={}", clientIp, ipCount);
                throw new BusinessException(ErrorCode.OPERATION_TOO_FREQUENT, "当前网络验证码发送次数已达上限");
            }
        }

        // 4. 校验图形验证码（最后校验，避免上面的检查失败时提前消费 captchaKey）
        if (!graphCaptchaService.verify(captchaKey, captchaAnswer)) {
            throw new BusinessException(ErrorCode.AUTH_CAPTCHA_ERROR, "图形验证码错误或已过期");
        }

        // 5. 生成6位数字验证码
        String code = generateCode();

        // 6. 存入 Redis（使用统一小写的邮箱作为 key）
        String codeKey = CAPTCHA_PREFIX + normalizedEmail;
        stringRedisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 7. 设置发送间隔
        stringRedisTemplate.opsForValue().set(intervalKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 8. 递增单号每日发送次数（当天有效）
        if (dailyCount == 0) {
            stringRedisTemplate.opsForValue().set(dailyKey, "1", 1, TimeUnit.DAYS);
        } else {
            stringRedisTemplate.opsForValue().increment(dailyKey);
        }

        // 9. 递增 IP 每日发送次数
        if (clientIp != null) {
            String dailyIpKey = CAPTCHA_DAILY_IP_PREFIX + clientIp;
            if (ipCount == 0) {
                stringRedisTemplate.opsForValue().set(dailyIpKey, "1", 1, TimeUnit.DAYS);
            } else {
                stringRedisTemplate.opsForValue().increment(dailyIpKey);
            }
        }

        // 10. 异步发送邮件
        emailService.sendVerifyCode(email, code, purpose);

        log.info("验证码已发送: email={}, purpose={}", normalizedEmail, purpose);
    }

    /**
     * 校验验证码
     *
     * @param target 邮箱地址（会自动转为小写匹配）
     * @param code   验证码
     * @return 是否校验通过
     */
    public boolean verifyCode(String target, String code) {
        if (target == null || code == null) {
            return false;
        }
        String normalizedTarget = target.toLowerCase().trim();
        String codeKey = CAPTCHA_PREFIX + normalizedTarget;
        String storedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (storedCode == null || !storedCode.equals(code)) {
            return false;
        }
        // 验证通过后删除验证码，防止重复使用
        stringRedisTemplate.delete(codeKey);
        return true;
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            }
            // 多级代理时取第一个
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
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
