package com.zhixun.config;

import com.zhixun.common.annotation.OperationLog;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 操作日志AOP切面
 * 自动记录所有AdminController中的管理员操作
 * 以及带有 @OperationLog 注解的用户操作
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final SecurityUtil securityUtil;

    /**
     * 切点：AdminController下所有方法
     */
    @Pointcut("execution(* com.zhixun.controller.AdminController.*(..))")
    public void adminOperationPointcut() {
    }

    /**
     * 切点：带有 @OperationLog 注解的方法
     */
    @Pointcut("@annotation(com.zhixun.common.annotation.OperationLog)")
    public void annotatedOperationPointcut() {
    }

    /**
     * AdminController方法正常返回后记录操作日志
     */
    @AfterReturning(pointcut = "adminOperationPointcut()", returning = "result")
    public void afterAdminReturning(JoinPoint joinPoint, Object result) {
        recordLog(joinPoint, null);
    }

    /**
     * AdminController方法异常时记录操作日志
     */
    @AfterThrowing(pointcut = "adminOperationPointcut()", throwing = "ex")
    public void afterAdminThrowing(JoinPoint joinPoint, Exception ex) {
        recordLog(joinPoint, ex.getMessage());
    }

    /**
     * @OperationLog 注解方法正常返回后记录操作日志
     */
    @AfterReturning(pointcut = "annotatedOperationPointcut()", returning = "result")
    public void afterAnnotatedReturning(JoinPoint joinPoint, Object result) {
        recordLog(joinPoint, null);
    }

    /**
     * @OperationLog 注解方法异常时记录操作日志
     */
    @AfterThrowing(pointcut = "annotatedOperationPointcut()", throwing = "ex")
    public void afterAnnotatedThrowing(JoinPoint joinPoint, Exception ex) {
        recordLog(joinPoint, ex.getMessage());
    }

    /**
     * 异步记录操作日志
     */
    @Async
    public void recordLog(JoinPoint joinPoint, String errorMessage) {
        try {
            Long operatorId = securityUtil.getCurrentUserId();
            String ip = getClientIp();
            String module;
            String action;
            String targetType;
            Long targetId;

            // 优先使用 @OperationLog 注解的值
            OperationLog annotation = getOperationLogAnnotation(joinPoint);
            if (annotation != null) {
                module = annotation.module();
                action = annotation.action();
                targetType = resolveTargetTypeFromAnnotation(annotation);
            } else {
                module = resolveModule(joinPoint);
                action = resolveAction(joinPoint);
                targetType = resolveTargetType(joinPoint);
            }
            targetId = resolveTargetId(joinPoint);
            String detail = buildDetail(joinPoint, errorMessage);

            operationLogService.log(operatorId, module, action, targetType, targetId, detail, ip);
        } catch (Exception e) {
            log.error("AOP记录操作日志失败: {}", e.getMessage());
        }
    }

    /**
     * 获取方法上的 @OperationLog 注解
     */
    private OperationLog getOperationLogAnnotation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getAnnotation(OperationLog.class);
    }

    /**
     * 从注解推断目标类型
     */
    private String resolveTargetTypeFromAnnotation(OperationLog annotation) {
        String module = annotation.module();
        if (module.contains("作品")) return "ARTICLE";
        if (module.contains("评论")) return "COMMENT";
        if (module.contains("关注")) return "FOLLOW";
        if (module.contains("点赞")) return "LIKE";
        if (module.contains("收藏")) return "COLLECT";
        if (module.contains("设置")) return "SETTINGS";
        if (module.contains("认证")) return "AUTH";
        if (module.contains("标签")) return "TAG";
        if (module.contains("用户")) return "USER";
        return "SYSTEM";
    }

    /**
     * 解析操作模块（根据方法名推断，用于AdminController）
     */
    private String resolveModule(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();

        if (methodName.contains("Article") || methodName.contains("article")) {
            return "作品管理";
        } else if (methodName.contains("User") || methodName.contains("user")) {
            return "用户管理";
        } else if (methodName.contains("SensitiveWord") || methodName.contains("sensitive")) {
            return "敏感词管理";
        } else if (methodName.contains("Comment") || methodName.contains("comment")) {
            return "评论管理";
        } else if (methodName.contains("Dashboard") || methodName.contains("dashboard")) {
            return "数据概览";
        } else if (methodName.contains("Search") || methodName.contains("search")) {
            return "搜索管理";
        } else if (methodName.contains("Log") || methodName.contains("log")) {
            return "日志管理";
        }
        return "系统管理";
    }

    /**
     * 解析操作动作
     */
    private String resolveAction(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();

        if (methodName.startsWith("get") || methodName.startsWith("list") || methodName.startsWith("query")) {
            return "查询";
        } else if (methodName.startsWith("add") || methodName.startsWith("create") || methodName.startsWith("save")) {
            return "新增";
        } else if (methodName.startsWith("update") || methodName.startsWith("edit") || methodName.startsWith("modify")) {
            return "修改";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "删除";
        } else if (methodName.startsWith("audit") || methodName.startsWith("review")) {
            return "审核";
        } else if (methodName.startsWith("sync")) {
            return "同步";
        }
        return "操作";
    }

    /**
     * 解析目标类型
     */
    private String resolveTargetType(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();

        if (methodName.contains("Article") || methodName.contains("article")) {
            return "ARTICLE";
        } else if (methodName.contains("User") || methodName.contains("user")) {
            return "USER";
        } else if (methodName.contains("SensitiveWord") || methodName.contains("sensitive")) {
            return "SENSITIVE_WORD";
        } else if (methodName.contains("Comment") || methodName.contains("comment")) {
            return "COMMENT";
        }
        return "SYSTEM";
    }

    /**
     * 解析目标ID（从方法参数中提取PathVariable的id值）
     */
    private Long resolveTargetId(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null && args != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                String paramName = parameterNames[i];
                if (("id".equals(paramName) || paramName.endsWith("Id"))
                        && args[i] instanceof Long) {
                    return (Long) args[i];
                }
            }
        }
        return null;
    }

    /**
     * 构建操作详情
     */
    private String buildDetail(JoinPoint joinPoint, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("方法:").append(joinPoint.getSignature().toShortString());

        if (errorMessage != null) {
            sb.append(";异常:").append(errorMessage);
        }
        return sb.toString();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            // 多级代理时取第一个IP
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
