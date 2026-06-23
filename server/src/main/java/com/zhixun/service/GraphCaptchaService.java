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
        int answer = switch (operator) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "×" -> a * b;
            default -> 0;
        };
        String expression = a + " " + operator + " " + b + " = ?";

        // 生成唯一 key
        String captchaKey = UUID.randomUUID().toString().replace("-", "");

        // 存入 Redis
        stringRedisTemplate.opsForValue().set(
                GRAPH_CAPTCHA_PREFIX + captchaKey,
                String.valueOf(answer),
                EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

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
            return false;
        }
        String key = GRAPH_CAPTCHA_PREFIX + captchaKey;
        String storedAnswer = stringRedisTemplate.opsForValue().get(key);
        if (storedAnswer == null || !storedAnswer.equals(captchaAnswer.trim())) {
            return false;
        }
        // 验证通过后删除，防止重复使用
        stringRedisTemplate.delete(key);
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
