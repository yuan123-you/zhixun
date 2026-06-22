package com.zhixun.common.exception;

import com.zhixun.common.result.ErrorCode;
import lombok.Getter;

/**
 * 自定义业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 错误码 */
    private final int code;

    /**
     * 使用错误码构造
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 使用错误码 + 自定义消息构造
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    /**
     * 使用自定义状态码和消息构造
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
