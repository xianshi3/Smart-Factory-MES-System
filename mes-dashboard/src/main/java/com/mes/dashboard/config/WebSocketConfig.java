package com.mes.dashboard.config;

import com.mes.common.utils.JwtUtils;
import com.mes.dashboard.websocket.DashboardWebSocketHandler;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * WebSocket配置
 * @author MES
 * @description 配置WebSocket端点（握手时校验 JWT Token，防未授权订阅实时数据）
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final DashboardWebSocketHandler dashboardWebSocketHandler;
    private final JwtUtils jwtUtils;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(dashboardWebSocketHandler, "/ws/dashboard")
                .addInterceptors(new JwtHandshakeInterceptor(jwtUtils))
                .setAllowedOriginPatterns("*");
    }

    /**
     * 握手拦截器：要求 URL 携带 ?token=xxx（有效 JWT），否则拒绝连接。
     * 浏览器 WebSocket API 无法自定义 Authorization 头，故采用 query 参数传递。
     */
    public static class JwtHandshakeInterceptor implements HandshakeInterceptor {

        private final JwtUtils jwtUtils;

        public JwtHandshakeInterceptor(JwtUtils jwtUtils) {
            this.jwtUtils = jwtUtils;
        }

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            String token = UriComponentsBuilder.fromUri(request.getURI())
                    .build().getQueryParams().getFirst("token");
            if (token == null || token.isBlank()) {
                log.warn("WebSocket 握手失败: 缺少 token，uri={}", request.getURI());
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            try {
                var claims = jwtUtils.parseToken(token);
                attributes.put("userId", claims.getSubject());
                attributes.put("username", claims.get("username", String.class));
                return true;
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("WebSocket 握手失败: token 无效，uri={}", request.getURI());
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
            // no-op
        }
    }
}
