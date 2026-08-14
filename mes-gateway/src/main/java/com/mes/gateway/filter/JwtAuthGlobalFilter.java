package com.mes.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

/**
 * 网关全局 JWT 鉴权过滤器。
 * - 白名单（登录/注册/CORS 预检/健康检查）直接放行；
 * - 其余请求必须携带有效 Bearer Token，否则返回 401；
 * - 校验通过后向后端透传 X-User-Id / X-User-Name / X-User-Role 请求头。
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = Logger.getLogger(JwtAuthGlobalFilter.class.getName());
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${mes.security.whitelist:/api/auth/login,/api/auth/register,/actuator/**}")
    private String whitelist;

    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "网关 JWT 签名密钥未配置或过短: 请设置环境变量 JWT_SECRET（长度 >= 32），禁止使用默认值启动");
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod().name()) || isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return reject(exchange, "未登录或Token缺失");
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        Claims claims;
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return reject(exchange, "登录已过期，请重新登录");
        }

        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        ServerHttpRequest mutated = request.mutate()
                .headers(headers -> {
                    headers.set("X-User-Id", userId == null ? "" : userId);
                    if (username != null) {
                        headers.set("X-User-Name", username);
                    }
                    if (role != null) {
                        headers.set("X-User-Role", role);
                    }
                })
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isWhitelisted(String path) {
        if (whitelist == null || whitelist.isBlank()) {
            return false;
        }
        List<String> paths = List.of(whitelist.split(","));
        return paths.stream().anyMatch(p -> {
            String trim = p.trim();
            if (trim.endsWith("/**")) {
                return path.startsWith(trim.substring(0, trim.length() - 3));
            }
            return path.equals(trim) || path.startsWith(trim + "/");
        });
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":5002,\"message\":\"" + message + "\",\"data\":null,\"timestamp\":"
                + System.currentTimeMillis() + "}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
