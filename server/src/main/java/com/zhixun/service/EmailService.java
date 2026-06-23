package com.zhixun.service;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    /**
     * 发送验证码邮件
     *
     * @param toEmail 收件人邮箱
     * @param code    验证码
     * @param purpose 用途（注册/登录/重置密码等）
     */
    public void sendVerifyCode(String toEmail, String code, String purpose) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【知讯】" + purpose + "验证码");
            message.setText("您正在进行" + purpose + "操作，验证码为：" + code + "，5分钟内有效，请勿泄露给他人。");
            mailSender.send(message);
            log.info("验证码邮件发送成功: to={}, purpose={}", toEmail, purpose);
        } catch (Exception e) {
            log.error("验证码邮件发送失败: to={}, purpose={}", toEmail, purpose, e);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮件发送失败，请稍后重试");
        }
    }
}
