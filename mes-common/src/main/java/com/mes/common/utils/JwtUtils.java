package com.mes.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 * 用于生成和解析JSON Web Token
 */
@Component
public class JwtUtils {

    /** 最小密钥长度（HS256 要求 >= 32 字节） */
    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${jwt.secret:}")
    private String secret;

    /** 过期时间（毫秒），默认24小时 */
    @Value("${jwt.expiration:86400000}")
    private long expiration;

    /**
     * 启动校验：拒绝空密钥/过短密钥，防止使用公开默认值运行。
     * 部署时必须在环境变量或配置中提供 JWT_SECRET（>= 32 字符）。
     */
    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT 签名密钥未配置或过短: 请设置环境变量 JWT_SECRET（长度 >= " + MIN_SECRET_LENGTH
                            + "），禁止使用代码内默认密钥启动服务");
        }
    }

    /**
     * 获取密钥对象
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT令牌
     * @param userId 用户ID
     * @param username 用户名
     * @param claims 自定义声明
     * @return JWT令牌字符串
     */
    public String generateToken(Long userId, String username, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 解析JWT令牌
     * @param token 令牌字符串
     * @return Claims声明对象
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取用户ID
     * @param token 令牌字符串
     * @return 用户ID
     */
    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    /**
     * 判断令牌是否过期
     * @param token 令牌字符串
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }

    /**
     * 获取令牌剩余有效时间（毫秒）
     * @param token 令牌字符串
     * @return 剩余毫秒数，负数表示已过期
     */
    public long getRemainingMillis(String token) {
        return parseToken(token).getExpiration().getTime() - System.currentTimeMillis();
    }
}
