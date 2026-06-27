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
    PARAMS_ERROR(4001, "参数错误"),
    UNAUTHORIZED(401, "请先登录"),
    FORBIDDEN(403, "暂无操作权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "不支持的请求方式"),
    CONFLICT(409, "资源冲突，请稍后重试"),
    GONE(410, "资源已被删除"),
    UNPROCESSABLE_ENTITY(422, "提交信息不完整，请检查后重试"),
    TOO_MANY_REQUESTS(429, "操作太频繁，请稍后重试"),

    // ========== 服务端错误码 5xx ==========
    SERVICE_UNAVAILABLE(503, "服务正在维护中"),

    // ========== 认证相关 1xxx ==========
    AUTH_LOGIN_FAILED(1001, "登录失败，请检查账号密码"),
    AUTH_ACCOUNT_DISABLED(1002, "账号已被限制使用"),
    AUTH_TOKEN_EXPIRED(1003, "登录信息已过期，请重新登录"),
    AUTH_TOKEN_INVALID(1004, "登录信息已过期，请重新登录"),
    AUTH_REFRESH_TOKEN_EXPIRED(1005, "登录信息已过期，请重新登录"),
    AUTH_PHONE_NOT_VERIFIED(1006, "手机号码未验证"),
    AUTH_CAPTCHA_ERROR(1007, "验证码输入错误"),

    // ========== 用户相关 2xxx ==========
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_PHONE_EXISTS(2002, "手机号已注册"),
    USER_EMAIL_EXISTS(2003, "邮箱已注册"),
    USER_PASSWORD_ERROR(2004, "密码输入错误"),
    USER_OLD_PASSWORD_ERROR(2005, "原密码输入错误"),

    // ========== 工单相关 3xxx ==========
    WORK_ORDER_NOT_FOUND(3001, "工单不存在"),
    WORK_ORDER_STATUS_ERROR(3002, "工单状态不正确"),
    WORK_ORDER_ASSIGNED(3003, "工单已分配"),
    WORK_ORDER_CLOSED(3004, "工单已关闭"),

    // ========== 文件相关 4xxx ==========
    FILE_UPLOAD_FAILED(4001, "文件上传失败，请重试"),
    FILE_TYPE_NOT_ALLOWED(4002, "不支持该文件格式"),
    FILE_SIZE_EXCEEDED(4003, "文件大小超出限制"),

    // ========== 业务错误 5xxx ==========
    SENSITIVE_WORD_DETECTED(5001, "内容包含不合规信息，请修改后重试"),
    OPERATION_TOO_FREQUENT(5002, "操作太频繁，请稍后重试"),
    BUSINESS_ERROR(5003, "操作失败，请稍后重试");

    /** 状态码 */
    private final int code;

    /** 提示信息 */
    private final String message;
}
