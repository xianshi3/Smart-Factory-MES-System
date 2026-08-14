package com.mes.common.security;

/**
 * Token 黑名单检查（登出/改密后的 Token 失效）。
 * 由提供黑名单存储的服务（如 mes-auth 基于 Redis）实现；
 * 未提供实现的服务自动跳过黑名单检查。
 */
public interface TokenBlacklistChecker {

    boolean isTokenBlacklisted(String token);
}