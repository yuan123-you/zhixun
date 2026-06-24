package com.zhixun.service;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码服务（算术验证码）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphCaptchaService {

    private final StringRedisTemplate stringRedisTemplate;

    /** 图形验证码 Redis Key 前缀 */
    private static final String GRAPH_CAPTCHA_PREFIX = "auth:graph:captcha:";

    /** 图形验证码有效期（分钟） */
    private static final long EXPIRE_MINUTES = 5;

    /** 本地缓存降级（Redis不可用时使用） */
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    /** 本地缓存条目 */
    private record CacheEntry(String value, long expireAt) {
        boolean isExpired() { return System.currentTimeMillis() > expireAt; }
    }

    /** 图片宽度 */
    private static final int WIDTH = 120;

    /** 图片高度 */
    private static final int HEIGHT = 40;

    /**
     * 生成图形验证码
     *
     * @return captchaKey 和 Base64 图片
     */
    public GraphCaptchaResult generate() {
        // 生成算术表达式
        int a = (int) (Math.random() * 20) + 1;
        int b = (int) (Math.random() * 20) + 1;
        String[] operators = {"+", "-", "×"};
        String operator = operators[(int) (Math.random() * operators.length)];
        // 减法确保结果非负，避免用户输入负号时的混淆
        int answer = switch (operator) {
            case "+" -> a + b;
            case "-" -> Math.max(a, b) - Math.min(a, b);
            case "×" -> a * b;
            default -> 0;
        };
        // 减法时确保大数在前，表达式与答案一致
        String expression = (operator.equals("-") ? Math.max(a, b) : a) + " " + operator + " " + (operator.equals("-") ? Math.min(a, b) : b) + " = ?";

        // 生成唯一 key
        String captchaKey = UUID.randomUUID().toString().replace("-", "");

        // 存入 Redis（降级到本地缓存）
        String key = GRAPH_CAPTCHA_PREFIX + captchaKey;
        try {
            stringRedisTemplate.opsForValue().set(key, String.valueOf(answer), EXPIRE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis不可用，图形验证码降级到本地缓存: {}", e.getMessage());
            localCache.put(key, new CacheEntry(String.valueOf(answer),
                    System.currentTimeMillis() + EXPIRE_MINUTES * 60 * 1000));
        }

        // 生成图片
        String imageBase64 = generateImage(expression);

        return new GraphCaptchaResult(captchaKey, imageBase64);
    }

    /**
     * 校验图形验证码
     *
     * @param captchaKey    验证码键
     * @param captchaAnswer 用户输入的答案
     * @return 是否校验通过
     */
    public boolean verify(String captchaKey, String captchaAnswer) {
        if (captchaKey == null || captchaKey.isBlank() || captchaAnswer == null || captchaAnswer.isBlank()) {
            log.warn("图形验证码校验失败: 参数为空, captchaKey={}, captchaAnswer={}",
                    captchaKey != null ? "present" : "null",
                    captchaAnswer != null ? "present" : "null");
            return false;
        }
        String key = GRAPH_CAPTCHA_PREFIX + captchaKey;
        String storedAnswer = null;
        try {
            storedAnswer = stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            // Redis不可用，从本地缓存获取
            log.warn("Redis不可用，从本地缓存获取图形验证码: {}", e.getMessage());
            CacheEntry entry = localCache.get(key);
            if (entry != null && !entry.isExpired()) {
                storedAnswer = entry.value;
            }
        }
        if (storedAnswer == null) {
            log.warn("图形验证码校验失败: 验证码不存在或已过期, key={}", key);
            return false;
        }
        // 数值比较：避免全角数字、前导零、Unicode减号等字符串匹配问题
        try {
            int expected = Integer.parseInt(storedAnswer.trim());
            String normalizedAnswer = captchaAnswer.trim()
                    .replace('−', '-')   // Unicode减号 → ASCII连字符
                    .replace('＋', '+')  // Unicode加号 → ASCII加号
                    .replace('×', '*')   // 中文乘号 → 星号（虽然答案不会包含）
                    .replaceAll("[\\s\\u00A0]+", ""); // 去除空格和不换行空格
            int actual = Integer.parseInt(normalizedAnswer);
            if (expected != actual) {
                log.warn("图形验证码校验失败: 答案不匹配, expected={}, actual={}", expected, actual);
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("图形验证码校验失败: 答案格式无效, storedAnswer={}, captchaAnswer={}", storedAnswer, captchaAnswer);
            return false;
        }
        // 验证通过后删除，防止重复使用
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            localCache.remove(key);
        }
        return true;
    }

    /**
     * 生成带干扰线的算术验证码图片
     */
    private String generateImage(String expression) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 边框
        g.setColor(Color.LIGHT_GRAY);
        g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);

        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(randomColor());
            g.drawLine(
                    (int) (Math.random() * WIDTH), (int) (Math.random() * HEIGHT),
                    (int) (Math.random() * WIDTH), (int) (Math.random() * HEIGHT)
            );
        }

        // 干扰点
        for (int i = 0; i < 30; i++) {
            image.setRGB(
                    (int) (Math.random() * WIDTH),
                    (int) (Math.random() * HEIGHT),
                    randomColor().getRGB()
            );
        }

        // 绘制文字
        g.setColor(new Color(30, 30, 30));
        g.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(expression);
        int x = (WIDTH - textWidth) / 2;
        int y = (HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
        // 微调文字位置，增加识别难度
        g.drawString(expression, x, y);

        g.dispose();

        // 转为 Base64
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图形验证码生成失败");
        }
    }

    /**
     * 生成随机颜色
     */
    private Color randomColor() {
        return new Color(
                (int) (Math.random() * 200),
                (int) (Math.random() * 200),
                (int) (Math.random() * 200)
        );
    }

    /**
     * 图形验证码结果
     */
    public record GraphCaptchaResult(String captchaKey, String image) {
    }
}
