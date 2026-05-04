package com.mes.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.entity.Permission;
import com.mes.common.mapper.PermissionMapper;
import com.mes.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器
 */
@RestController
@RequestMapping("/auth/permission")
@RequiredArgsConstructor
@Tag(name = "权限管理", description = "权限管理接口")
public class PermissionController {

    private final PermissionMapper permissionMapper;

    /**
     * 获取所有权限列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有权限")
    public Result<List<Permission>> list() {
        List<Permission> permissions = permissionMapper.selectList(
            new LambdaQueryWrapper<Permission>()
                .orderByAsc(Permission::getSort)
        );
        return Result.ok(permissions);
    }

    /**
     * 创建权限
     */
    @PostMapping
    @Operation(summary = "创建权限")
    public Result<Void> create(@RequestBody Permission permission) {
        permissionMapper.insert(permission);
        return Result.ok();
    }

    /**
     * 更新权限
     */
    @PutMapping
    @Operation(summary = "更新权限")
    public Result<Void> update(@RequestBody Permission permission) {
        permissionMapper.updateById(permission);
        return Result.ok();
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除权限")
    public Result<Void> delete(@PathVariable Long id) {
        permissionMapper.deleteById(id);
        return Result.ok();
    }
}