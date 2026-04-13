package com.mes.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.entity.Menu;
import com.mes.common.mapper.MenuMapper;
import com.mes.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单管理控制器
 */
@RestController
@RequestMapping("/auth/menu")
@RequiredArgsConstructor
@Tag(name = "菜单管理", description = "菜单查询接口")
public class MenuController {

    private final MenuMapper menuMapper;

    /**
     * 获取当前用户菜单
     */
    @GetMapping("/user")
    @Operation(summary = "获取当前用户菜单")
    public Result<List<Menu>> userMenus() {
        List<Menu> allMenus = menuMapper.selectList(
            new LambdaQueryWrapper<Menu>()
                .eq(Menu::getStatus, 1)
                .orderByAsc(Menu::getSort)
        );
        
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
}