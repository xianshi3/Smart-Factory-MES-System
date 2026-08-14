package com.mes.common.security;

import com.mes.common.entity.Permission;
import com.mes.common.entity.SysUserAuth;
import com.mes.common.mapper.PermissionMapper;
import com.mes.common.mapper.RolePermissionMapper;
import com.mes.common.mapper.SysUserAuthMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 权限码查询：按 sys_user.role_id → sys_role_permission → sys_permission 解析当前用户权限码，
 * 带 5 分钟本地缓存。ADMIN 角色视为拥有全部权限。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    /** 权限缓存有效期（毫秒） */
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    private final SysUserAuthMapper sysUserAuthMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;

    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * 获取用户拥有的权限码集合（未登录返回空集）
     */
    public Set<String> permissionsOf(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        CacheEntry entry = cache.get(userId);
        long now = System.currentTimeMillis();
        if (entry != null && entry.expireAt > now) {
            return entry.codes;
        }
        Set<String> codes = loadFromDb(userId);
        cache.put(userId, new CacheEntry(codes, now + CACHE_TTL_MS));
        return codes;
    }

    /**
     * 当前登录用户是否拥有指定权限码；ADMIN 默认放行
     */
    public boolean hasPermission(String permissionCode) {
        CurrentUser user = UserContext.get();
        if (user == null) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        return permissionsOf(user.getUserId()).contains(permissionCode);
    }

    /**
     * 当前登录用户是否属于指定角色之一
     */
    public boolean hasAnyRole(String... roles) {
        CurrentUser user = UserContext.get();
        if (user == null) {
            return false;
        }
        for (String role : roles) {
            if (role != null && role.equalsIgnoreCase(user.getRole())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清理指定用户的权限缓存（角色/权限变更后调用）
     */
    public void evict(Long userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    private Set<String> loadFromDb(Long userId) {
        try {
            SysUserAuth user = sysUserAuthMapper.selectById(userId);
            if (user == null) {
                return Collections.emptySet();
            }
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                return permissionMapper.selectList(null).stream()
                        .map(Permission::getPermissionCode)
                        .filter(c -> c != null && !c.isBlank())
                        .collect(Collectors.toSet());
            }
            if (user.getRoleId() == null) {
                return Collections.emptySet();
            }
            List<Long> permissionIds = rolePermissionMapper.selectPermissionIdsByRoleId(user.getRoleId());
            if (permissionIds == null || permissionIds.isEmpty()) {
                return Collections.emptySet();
            }
            List<Permission> permissions = permissionMapper.selectBatchIds(permissionIds);
            if (permissions == null) {
                return Collections.emptySet();
            }
            return permissions.stream()
                    .map(Permission::getPermissionCode)
                    .filter(c -> c != null && !c.isBlank())
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("查询用户[{}]权限失败: {}", userId, e.getMessage());
            return Collections.emptySet();
        }
    }

    private record CacheEntry(Set<String> codes, long expireAt) {
    }
}