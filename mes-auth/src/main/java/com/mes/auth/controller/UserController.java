package com.mes.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.auth.entity.User;
import com.mes.auth.mapper.UserMapper;
import com.mes.common.result.Result;
import com.mes.common.security.PermissionService;
import com.mes.common.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理接口（仅管理员）
 */
@RestController
@RequestMapping("/auth/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户CRUD接口")
@RequireRole("ADMIN")
public class UserController {

    private final UserMapper userMapper;
    private final PermissionService permissionService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 获取用户列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取用户列表")
    public Result<List<User>> list() {
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .orderByDesc(User::getCreateTime)
        );
        
        for (User user : users) {
            user.setPassword(null);
        }
        
        return Result.ok(users);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情")
    public Result<User> getById(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.ok(user);
    }

    /**
     * 创建用户
     */
    @PostMapping
    @Operation(summary = "创建用户")
    public Result<Void> create(@RequestBody User user) {
        if (!StringUtils.hasText(user.getPassword())) {
            return Result.fail(400, "密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        return Result.ok();
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户")
    public Result<Void> update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            // 未传密码时保持原密码不变
            user.setPassword(null);
        }
        userMapper.updateById(user);
        return Result.ok();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        userMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 分配角色
     */
    @PutMapping("/{id}/role")
    @Operation(summary = "分配角色")
    public Result<Void> assignRole(@PathVariable Long id, @RequestBody User user) {
        User existingUser = userMapper.selectById(id);
        if (existingUser != null) {
            existingUser.setRole(user.getRole());
            userMapper.updateById(existingUser);
            // 角色变更后立即清除该用户的权限缓存
            permissionService.evict(id);
        }
        return Result.ok();
    }
}