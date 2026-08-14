package com.mes.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.entity.Menu;
import com.mes.common.mapper.MenuMapper;
import com.mes.common.result.Result;
import com.mes.common.security.PermissionService;
import com.mes.common.security.RequireRole;
import com.mes.common.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单管理控制器
 */
@RestController
@RequestMapping("/auth/menu")
@RequiredArgsConstructor
@Tag(name = "菜单管理", description = "菜单管理接口")
public class MenuController {

    private final MenuMapper menuMapper;
    private final PermissionService permissionService;

    /**
     * 获取当前用户菜单（按角色权限码过滤，ADMIN 全量）
     */
    @GetMapping("/user")
    @Operation(summary = "获取当前用户菜单")
    public Result<List<Menu>> userMenus() {
        List<Menu> allMenus = menuMapper.selectList(
            new LambdaQueryWrapper<Menu>()
                .eq(Menu::getStatus, 1)
                .orderByAsc(Menu::getSort)
        );

        Set<String> allowedCodes = null;
        if (UserContext.isAuthenticated() && !"ADMIN".equalsIgnoreCase(UserContext.getRole())) {
            allowedCodes = permissionService.permissionsOf(UserContext.getUserId());
            Set<String> allowed = allowedCodes;
            allMenus = allMenus.stream()
                    .filter(m -> allowed.contains(m.getMenuCode()))
                    .collect(Collectors.toList());
            // 补回被隐藏子菜单的父菜单
            Set<Long> childIds = allMenus.stream()
                    .map(Menu::getParentId)
                    .filter(p -> p != null && p > 0)
                    .collect(Collectors.toSet());
            if (!childIds.isEmpty()) {
                allMenus = menuMapper.selectList(
                        new LambdaQueryWrapper<Menu>()
                                .eq(Menu::getStatus, 1)
                                .in(Menu::getId, childIds)
                );
            }
        }

        List<Menu> topMenus = allMenus.stream()
            .filter(m -> m.getParentId() == null || m.getParentId() == 0)
            .collect(Collectors.toList());
        
        for (Menu menu : topMenus) {
            List<Menu> children = allMenus.stream()
                .filter(m -> menu.getId().equals(m.getParentId()))
                .collect(Collectors.toList());
            if (!children.isEmpty()) {
                menu.setChildren(children);
            }
        }
        
        return Result.ok(topMenus);
    }

    /**
     * 获取所有菜单列表（管理员）
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有菜单")
    @RequireRole("ADMIN")
    public Result<List<Menu>> list() {
        List<Menu> menus = menuMapper.selectList(
            new LambdaQueryWrapper<Menu>()
                .orderByAsc(Menu::getSort)
        );
        return Result.ok(menus);
    }
    
    /**
     * 根据角色ID获取菜单
     */
    @GetMapping("/role/{roleId}")
    @Operation(summary = "根据角色获取菜单")
    @RequireRole("ADMIN")
    public Result<List<Menu>> roleMenus(@PathVariable Long roleId) {
        List<Menu> menus = menuMapper.selectList(
            new LambdaQueryWrapper<Menu>()
                .eq(Menu::getStatus, 1)
                .orderByAsc(Menu::getSort)
        );
        
        List<Menu> topMenus = menus.stream()
            .filter(m -> m.getParentId() == null || m.getParentId() == 0)
            .collect(Collectors.toList());
        
        for (Menu menu : topMenus) {
            List<Menu> children = menus.stream()
                .filter(m -> menu.getId().equals(m.getParentId()))
                .collect(Collectors.toList());
            if (!children.isEmpty()) {
                menu.setChildren(children);
            }
        }
        
        return Result.ok(topMenus);
    }

    /**
     * 创建菜单
     */
    @PostMapping
    @Operation(summary = "创建菜单")
    @RequireRole("ADMIN")
    public Result<Void> create(@RequestBody Menu menu) {
        menuMapper.insert(menu);
        return Result.ok();
    }

    /**
     * 更新菜单
     */
    @PutMapping
    @Operation(summary = "更新菜单")
    @RequireRole("ADMIN")
    public Result<Void> update(@RequestBody Menu menu) {
        menuMapper.updateById(menu);
        return Result.ok();
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除菜单")
    @RequireRole("ADMIN")
    public Result<Void> delete(@PathVariable Long id) {
        menuMapper.deleteById(id);
        return Result.ok();
    }
}
