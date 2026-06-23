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
     * 异步发送通知邮件
     *
     * @param toEmail 收件人邮箱
     * @param subject 邮件主题
     * @param text    邮件内容
     */
    @Async
    public void sendNotificationEmail(String toEmail, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("通知邮件发送成功: to={}", toEmail);
        } catch (Exception e) {
            log.error("通知邮件发送失败: to={}", toEmail, e);
        }
    }

    /**
     * 异步发送验证码邮件
     *
     * @param toEmail 收件人邮箱
     * @param code    验证码
     * @param purpose 用途（注册/登录/重置密码等）
     */
    @Async
    public void sendVerifyCode(String toEmail, String code, String purpose) {
        try {
            String purposeText = switch (purpose) {
                case "register" -> "注册账号";
                case "login" -> "登录系统";
                case "resetPassword" -> "重置密码";
                default -> purpose;
            };

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("【知讯】" + purposeText + "验证码");
            message.setText("尊敬的用户，您好！\n\n"
                    + "您正在使用知讯平台进行「" + purposeText + "」操作，本次验证码为：\n\n"
                    + "    " + code + "\n\n"
                    + "验证码有效期为5分钟，请在有效期内完成验证。验证码仅限本次操作使用，切勿转发或泄露给他人。\n\n"
                    + "如非本人操作，请忽略此邮件，您的账号安全不会受到影响。如有疑问，请联系知讯平台客服。\n\n"
                    + "此邮件由知讯平台系统自动发送，请勿直接回复。");
            mailSender.send(message);
            log.info("验证码邮件发送成功: to={}, purpose={}", toEmail, purpose);
        } catch (Exception e) {
            log.error("验证码邮件发送失败: to={}, purpose={}", toEmail, purpose, e);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮件发送失败，请稍后重试");
        }
    }
}
