package com.zhixun.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ========== 通用错误码 2xx ==========
    SUCCESS(200, "success"),
    CREATED(201, "资源创建成功"),

    // ========== 客户端错误码 4xx ==========
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "请求资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    GONE(410, "资源已被删除"),
    UNPROCESSABLE_ENTITY(422, "参数校验失败"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    // ========== 认证相关 1xxx ==========
    AUTH_LOGIN_FAILED(1001, "用户名或密码错误"),
    AUTH_ACCOUNT_DISABLED(1002, "账号已被禁用"),
    AUTH_TOKEN_EXPIRED(1003, "令牌已过期"),
    AUTH_TOKEN_INVALID(1004, "令牌无效"),
    AUTH_REFRESH_TOKEN_EXPIRED(1005, "刷新令牌已过期"),
    AUTH_PHONE_NOT_VERIFIED(1006, "手机号未验证"),
    AUTH_CAPTCHA_ERROR(1007, "验证码错误"),

    // ========== 用户相关 2xxx ==========
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_PHONE_EXISTS(2002, "手机号已注册"),
    USER_EMAIL_EXISTS(2003, "邮箱已注册"),
    USER_PASSWORD_ERROR(2004, "密码错误"),
    USER_OLD_PASSWORD_ERROR(2005, "原密码错误"),

    // ========== 工单相关 3xxx ==========
    WORK_ORDER_NOT_FOUND(3001, "工单不存在"),
    WORK_ORDER_STATUS_ERROR(3002, "工单状态不正确"),
    WORK_ORDER_ASSIGNED(3003, "工单已分配"),
    WORK_ORDER_CLOSED(3004, "工单已关闭"),

    // ========== 文件相关 4xxx ==========
    FILE_UPLOAD_FAILED(4001, "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED(4002, "文件类型不允许"),
    FILE_SIZE_EXCEEDED(4003, "文件大小超限"),

    // ========== 业务错误 5xxx ==========
    SENSITIVE_WORD_DETECTED(5001, "内容包含敏感词"),
    OPERATION_TOO_FREQUENT(5002, "操作过于频繁"),
    BUSINESS_ERROR(5003, "业务处理失败");

    /** 状态码 */
    private final int code;

    /** 提示信息 */
    private final String message;
}
