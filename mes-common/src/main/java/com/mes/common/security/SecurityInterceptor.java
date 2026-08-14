package com.mes.common.security;

import com.mes.common.exception.BizException;
import com.mes.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 权限校验拦截器（替代 AOP 切面，零 AspectJ 依赖）。
 * 读取方法级/类级 {@link RequireRole} 与 {@link RequirePermission} 注解并校验，
 * 方法级注解优先于类级注解。
 */
@RequiredArgsConstructor
public class SecurityInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole roleAnn = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (roleAnn == null) {
            roleAnn = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (roleAnn != null) {
            if (!UserContext.isAuthenticated()) {
                throw new BizException(ErrorCode.UNAUTHORIZED);
            }
            if (!permissionService.hasAnyRole(roleAnn.value())) {
                throw new BizException(ErrorCode.FORBIDDEN);
            }
        }

        RequirePermission permAnn = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (permAnn == null) {
            permAnn = handlerMethod.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (permAnn != null) {
            if (!UserContext.isAuthenticated()) {
                throw new BizException(ErrorCode.UNAUTHORIZED);
            }
            if (!permissionService.hasPermission(permAnn.value())) {
                throw new BizException(ErrorCode.FORBIDDEN);
            }
        }
        return true;
    }
}