package com.zhixun.common.exception;

import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.R;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * 根据业务错误码映射到合适的 HTTP 状态码，避免一律返回 200 导致客户端无法区分错误类型
     */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e, HttpServletResponse response) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        response.setStatus(mapToHttpStatus(e.getCode()));
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 将业务错误码映射到 HTTP 状态码
     * 注意：不要将业务错误映射到 401，否则会触发客户端 token 刷新循环
     * 业务错误码始终通过 JSON body 的 code 字段传递给客户端
     */
    private int mapToHttpStatus(int code) {
        // 标准 HTTP 状态码直接透传（200-599）
        if (code >= 200 && code < 600) return code;
        // 权限相关 → 403 Forbidden
        if (code == ErrorCode.FORBIDDEN.getCode()) return 403;
        // 限流相关 → 429 Too Many Requests
        if (code == ErrorCode.TOO_MANY_REQUESTS.getCode()
                || code == ErrorCode.OPERATION_TOO_FREQUENT.getCode()) return 429;
        // 服务不可用 → 503
        if (code == ErrorCode.SERVICE_UNAVAILABLE.getCode()) return 503;
        // 认证令牌失效 → 401（触发客户端 token 刷新）
        if (code == ErrorCode.AUTH_TOKEN_EXPIRED.getCode()
                || code == ErrorCode.AUTH_TOKEN_INVALID.getCode()) return 401;
        // 其他所有业务异常 → 400 Bad Request
        // 客户端通过 JSON body 的 code/message 获取详细错误信息
        return 400;
    }

    /**
     * 处理参数校验异常 - @Valid/@Validated
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return R.fail(ErrorCode.UNPROCESSABLE_ENTITY, message);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败: {}", message);
        return R.fail(ErrorCode.UNPROCESSABLE_ENTITY, message);
    }

    /**
     * 处理约束违反异常 - @Validated 在路径变量/请求参数上
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("约束违反: {}", message);
        return R.fail(ErrorCode.UNPROCESSABLE_ENTITY, message);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("缺少请求参数: {}", e.getParameterName());
        return R.fail(ErrorCode.BAD_REQUEST, "缺少请求参数: " + e.getParameterName());
    }

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMethod());
        return R.fail(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("接口不存在: {}", e.getRequestURL());
        return R.fail(ErrorCode.NOT_FOUND);
    }

    /**
     * 处理认证异常 - 密码错误
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleBadCredentialsException(BadCredentialsException e) {
        log.warn("认证失败: {}", e.getMessage());
        return R.fail(ErrorCode.AUTH_LOGIN_FAILED);
    }

    /**
     * 处理授权异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("访问被拒绝: {}", e.getMessage());
        return R.fail(ErrorCode.FORBIDDEN);
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件大小超限: {}", e.getMessage());
        return R.fail(ErrorCode.FILE_SIZE_EXCEEDED);
    }

    /**
     * 处理 Sentinel 限流异常
     */
    @ExceptionHandler(FlowException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public R<Void> handleFlowException(FlowException e) {
        log.warn("接口限流: resource={}", e.getRule().getResource());
        return R.fail(ErrorCode.TOO_MANY_REQUESTS);
    }

    /**
     * 处理 Sentinel 熔断降级异常
     */
    @ExceptionHandler(DegradeException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public R<Void> handleDegradeException(DegradeException e) {
        log.warn("服务熔断降级: resource={}", e.getRule().getResource());
        return R.fail(503, "服务暂时不可用，请稍后重试");
    }

    /**
     * 处理 Sentinel 系统保护异常
     */
    @ExceptionHandler(SystemBlockException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public R<Void> handleSystemBlockException(SystemBlockException e) {
        log.warn("系统保护触发: rule={}", e.getRule());
        return R.fail(503, "系统负载过高，请稍后重试");
    }

    /**
     * 处理 Sentinel 其他阻塞异常
     */
    @ExceptionHandler(BlockException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public R<Void> handleBlockException(BlockException e) {
        log.warn("请求被阻塞: {}", e.getMessage());
        return R.fail(ErrorCode.TOO_MANY_REQUESTS);
    }

    /**
     * 处理请求体解析异常（JSON格式错误、类型不匹配等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = "请求数据格式错误";
        Throwable cause = e.getCause();
        if (cause instanceof InvalidFormatException ife) {
            message = "字段 '" + ife.getPath().stream()
                    .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."))
                    + "' 的值格式不正确";
        }
        log.warn("请求体解析异常: {}", e.getMessage());
        return R.fail(ErrorCode.BAD_REQUEST, message);
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return R.fail(500, "服务器内部错误");
    }
}
