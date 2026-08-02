package com.mes.auth.filter;

import com.mes.auth.service.AuthService;
import com.mes.common.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.equals("/auth/login") || path.equals("/auth/register")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.isEmpty()) {
            String token = authHeader.replace("Bearer ", "");
            try {
                Long userId = jwtUtils.getUserId(token);
                request.setAttribute("currentUserId", userId);
                // Token在黑名单中则拒绝（已登出/已改密的Token）
                if (authService.isTokenBlacklisted(token)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":5004,\"message\":\"Token已失效，请重新登录\"}");
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        chain.doFilter(request, response);
    }
}
