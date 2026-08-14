package com.mes.common.security;

/**
 * 请求线程内的登录用户上下文
 */
public class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.getUserId();
    }

    public static String getUsername() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.getUsername();
    }

    public static String getRole() {
        CurrentUser u = HOLDER.get();
        return u == null ? null : u.getRole();
    }

    /** 是否已登录（Token 有效并已解析） */
    public static boolean isAuthenticated() {
        CurrentUser u = HOLDER.get();
        return u != null && u.getUserId() != null;
    }

    public static void clear() {
        HOLDER.remove();
    }
}