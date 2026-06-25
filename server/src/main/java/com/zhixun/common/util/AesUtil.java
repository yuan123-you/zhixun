package com.zhixun.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM 加密工具类
 * 用于手机号、邮箱、私信等敏感信息加密
 */
@Slf4j
@Component
public class AesUtil {

    /** 加密算法 */
    private static final String ALGORITHM = "AES";

    /** 加密模式 - GCM */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /** GCM 认证标签长度（位） */
    private static final int GCM_TAG_LENGTH = 128;

    /** GCM 初始化向量长度（字节） */
    private static final int GCM_IV_LENGTH = 12;

    /** 密钥长度（字节）- 256位 */
    private static final int KEY_LENGTH = 32;

    /** 密钥 */
    @Value("${aes.secret-key:zhixun-aes-secret-key-2024-must-be-32b}")
    private String secretKey;

    /** 默认密钥（用于检测是否使用了不安全的默认配置） */
    private static final String DEFAULT_SECRET_KEY = "zhixun-aes-secret-key-2024-must-be-32b";

    /**
     * 初始化时验证 AES 密钥配置
     * 警告如果使用默认密钥或密钥强度不足
     */
    @PostConstruct
    public void validateSecretKey() {
        // 检查是否使用了默认密钥
        if (DEFAULT_SECRET_KEY.equals(secretKey)) {
            log.warn("⚠️  ⚠️  ⚠️  安全警告：当前使用默认 AES 密钥！请通过环境变量 AES_SECRET_KEY 配置强随机密钥！");
            log.warn("⚠️  生成强密钥命令：java -cp . KeyGenerator.java 或在 application.yml 中配置 Base64 编码的 32 字节随机密钥");
        }

        // 验证密钥长度（在 SHA-256 哈希之前）
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 16) {
            log.warn("AES 密钥长度不足 16 字节（当前：{} 字节），加密强度可能不足。建议使用至少 32 字节的随机密钥。", keyBytes.length);
        } else {
            log.info("AES 密钥配置验证通过，密钥字符串长度：{} 字节，哈希后长度：{} 字节（256 位）", keyBytes.length, KEY_LENGTH);
        }
    }

    /**
     * 加密明文
     *
     * @param plaintext 明文
     * @return Base64 编码的密文（包含 IV）
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            SecretKey key = getSecretKey();
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV 和密文
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES 加密失败: {}", e.getMessage());
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密密文
     *
     * @param ciphertext Base64 编码的密文（包含 IV）
     * @return 明文
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        try {
            SecretKey key = getSecretKey();
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            // 提取 IV 和密文
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES 解密失败: {}", e.getMessage());
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 获取 SecretKey
     * 通过 SHA-256 对密钥字符串进行哈希，确保密钥长度为 256 位
     */
    private SecretKey getSecretKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
        byte[] keyBytes = Arrays.copyOf(hash, KEY_LENGTH);
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
}
