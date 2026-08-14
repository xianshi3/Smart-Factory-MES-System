package com.mes.common.security;

import com.mes.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 全局 Token 认证过滤器（各服务组件扫描 com.mes.common 后自动生效）。
 * - 白名单（登录/注册）与 CORS 预检直接放行；
 * - 其余请求必须携带有效 Bearer Token，否则返回 401；
 * - 解析成功后写入 {@link UserContext}（请求线程内），业务层可取当前用户。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class TokenAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final ObjectProvider<TokenBlacklistChecker> blacklistCheckerProvider;

    @Value("${mes.security.whitelist:/auth/login,/auth/register}")
    private String whitelist;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            if (isWhitelisted(request.getRequestURI()) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
                chain.doFilter(request, response);
                return;
            }

            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                reject(response, "未登录或Token缺失");
                return;
            }
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();

            Claims claims;
            try {
                claims = jwtUtils.parseToken(token);
            } catch (JwtException | IllegalArgumentException e) {
                reject(response, "登录已过期，请重新登录");
                return;
            }

            TokenBlacklistChecker checker = blacklistCheckerProvider.getIfAvailable();
            if (checker != null && checker.isTokenBlacklisted(token)) {
                reject(response, "Token已失效，请重新登录");
                return;
            }

            Long userId;
            try {
                userId = Long.parseLong(claims.getSubject());
            } catch (NumberFormatException e) {
                reject(response, "Token无效");
                return;
            }
            CurrentUser user = new CurrentUser(
                    userId,
                    claims.get("username", String.class),
                    claims.get("role", String.class));
            UserContext.set(user);

            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private boolean isWhitelisted(String uri) {
        if (whitelist == null || whitelist.isBlank()) {
            return false;
        }
        List<String> paths = Arrays.asList(whitelist.split(","));
        return paths.stream().anyMatch(p -> uri.equals(p.trim()) || uri.startsWith(p.trim() + "/"));
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":5002,\"message\":\"" + message + "\",\"data\":null,\"timestamp\":"
                + System.currentTimeMillis() + "}");
    }
}