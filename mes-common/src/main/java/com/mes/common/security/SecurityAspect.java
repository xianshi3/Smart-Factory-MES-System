package com.mes.common.security;

import com.mes.common.exception.BizException;
import com.mes.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 权限注解切面：{@link RequireRole}（角色）与 {@link RequirePermission}（权限码）。
 * 方法级注解优先于类级注解。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class SecurityAspect {

    private final PermissionService permissionService;

    @Around("@annotation(requireRole) || @within(requireRole)")
    public Object checkRole(ProceedingJoinPoint pjp, RequireRole requireRole) throws Throwable {
        if (!UserContext.isAuthenticated()) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!permissionService.hasAnyRole(requireRole.value())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return pjp.proceed();
    }

    @Around("@annotation(requirePermission) || @within(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint pjp, RequirePermission requirePermission) throws Throwable {
        if (!UserContext.isAuthenticated()) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (!permissionService.hasPermission(requirePermission.value())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return pjp.proceed();
    }
}