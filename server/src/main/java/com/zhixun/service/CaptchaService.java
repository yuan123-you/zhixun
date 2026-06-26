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
import java.util.concurrent.ConcurrentHashMap;
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

    /** 本地缓存降级（Redis不可用时使用） */
    private final ConcurrentHashMap<String, LocalCodeEntry> localCodeCache = new ConcurrentHashMap<>();

    /** 本地验证码缓存条目 */
    private record LocalCodeEntry(String code, long expireAt) {
        boolean isExpired() { return System.currentTimeMillis() > expireAt; }
    }

    /** Redis是否可用（延迟判断，避免每次调用都尝试连接） */
    private volatile boolean redisAvailable = true;

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

        // 1-3. 频率限制检查（Redis可用时执行；不可用时跳过，仅日志告警）
        if (redisAvailable) {
            try {
                checkRateLimits(normalizedEmail);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Redis不可用，跳过频率限制检查: {}", e.getMessage());
                redisAvailable = false;
            }
        } else {
            // 尝试恢复Redis连接
            try {
                checkRateLimits(normalizedEmail);
                redisAvailable = true;
                log.info("Redis连接已恢复，频率限制检查重新启用");
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                // Redis仍不可用，继续降级流程
            }
        }

        // 4. 校验图形验证码（GraphCaptchaService 已有 Redis 降级能力）
        if (!graphCaptchaService.verify(captchaKey, captchaAnswer)) {
            throw new BusinessException(ErrorCode.AUTH_CAPTCHA_ERROR, "图形验证码错误或已过期");
        }

        // 5. 生成6位数字验证码
        String code = generateCode();

        // 6. 存储验证码（Redis可用时存Redis，否则存本地缓存）
        String codeKey = CAPTCHA_PREFIX + normalizedEmail;
        if (redisAvailable) {
            try {
                stringRedisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("Redis不可用，验证码降级到本地缓存: {}", e.getMessage());
                redisAvailable = false;
                long expireAt = System.currentTimeMillis() + CODE_EXPIRE_MINUTES * 60 * 1000;
                localCodeCache.put(codeKey, new LocalCodeEntry(code, expireAt));
            }
        } else {
            long expireAt = System.currentTimeMillis() + CODE_EXPIRE_MINUTES * 60 * 1000;
            localCodeCache.put(codeKey, new LocalCodeEntry(code, expireAt));
        }

        // 7-9. 设置发送间隔和每日计数（仅Redis可用时）
        if (redisAvailable) {
            try {
                String intervalKey = CAPTCHA_INTERVAL_PREFIX + normalizedEmail;
                stringRedisTemplate.opsForValue().set(intervalKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
                incrementDailyCounts(normalizedEmail);
            } catch (Exception e) {
                log.warn("Redis不可用，跳过计数记录: {}", e.getMessage());
                redisAvailable = false;
            }
        }

        // 10. 异步发送邮件
        emailService.sendVerifyCode(email, code, purpose);

        // 开发环境：邮件未配置时，将验证码输出到日志以便调试
        log.info("验证码已发送(email={}, purpose={})，验证码为: {}", normalizedEmail, purpose, code);
    }

    /**
     * 检查频率限制（Redis操作，可能抛异常）
     */
    private void checkRateLimits(String normalizedEmail) {
        // 1. 检查发送间隔
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
        if (clientIp != null) {
            String dailyIpKey = CAPTCHA_DAILY_IP_PREFIX + clientIp;
            String ipCountStr = stringRedisTemplate.opsForValue().get(dailyIpKey);
            int ipCount = ipCountStr != null ? Integer.parseInt(ipCountStr) : 0;
            if (ipCount >= DAILY_LIMIT_PER_IP) {
                log.warn("IP 每日发送上限触发: ip={}, count={}", clientIp, ipCount);
                throw new BusinessException(ErrorCode.OPERATION_TOO_FREQUENT, "当前网络验证码发送次数已达上限");
            }
        }
    }

    /**
     * 递增每日次数计数（Redis操作，可能抛异常）
     */
    private void incrementDailyCounts(String normalizedEmail) {
        String dailyKey = CAPTCHA_DAILY_PREFIX + normalizedEmail;
        String dailyCountStr = stringRedisTemplate.opsForValue().get(dailyKey);
        int dailyCount = dailyCountStr != null ? Integer.parseInt(dailyCountStr) : 0;
        if (dailyCount == 0) {
            stringRedisTemplate.opsForValue().set(dailyKey, "1", 1, TimeUnit.DAYS);
        } else {
            stringRedisTemplate.opsForValue().increment(dailyKey);
        }

        String clientIp = getClientIp();
        if (clientIp != null) {
            String dailyIpKey = CAPTCHA_DAILY_IP_PREFIX + clientIp;
            String ipCountStr = stringRedisTemplate.opsForValue().get(dailyIpKey);
            int ipCount = ipCountStr != null ? Integer.parseInt(ipCountStr) : 0;
            if (ipCount == 0) {
                stringRedisTemplate.opsForValue().set(dailyIpKey, "1", 1, TimeUnit.DAYS);
            } else {
                stringRedisTemplate.opsForValue().increment(dailyIpKey);
            }
        }
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
        String storedCode = null;
        try {
            storedCode = stringRedisTemplate.opsForValue().get(codeKey);
        } catch (Exception e) {
            // Redis不可用，从本地缓存获取
            log.warn("Redis不可用，从本地缓存获取验证码: {}", e.getMessage());
            LocalCodeEntry entry = localCodeCache.get(codeKey);
            if (entry != null && !entry.isExpired()) {
                storedCode = entry.code;
            }
        }
        if (storedCode == null || !storedCode.equals(code)) {
            return false;
        }
        // 验证通过后删除验证码，防止重复使用
        try {
            stringRedisTemplate.delete(codeKey);
        } catch (Exception e) {
            localCodeCache.remove(codeKey);
        }
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
