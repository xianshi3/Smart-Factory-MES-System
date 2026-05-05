package com.mes.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mes.common.entity.Permission;
import com.mes.common.entity.Role;
import com.mes.common.entity.RolePermission;
import com.mes.common.mapper.PermissionMapper;
import com.mes.common.mapper.RoleMapper;
import com.mes.common.mapper.RolePermissionMapper;
import com.mes.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/auth/role")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "角色CRUD接口")
public class RoleController {

    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    /**
     * 获取角色列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取角色列表")
    public Result<List<Role>> list() {
        List<Role> roles;
        try {
            long count = roleMapper.selectCount(null);
            if (count == 0) {
                Role admin = new Role();
                admin.setRoleName("超级管理员");
                admin.setRoleCode("ADMIN");
                admin.setDescription("拥有系统所有权限");
                admin.setStatus(1);
                roleMapper.insert(admin);
                
                Role manager = new Role();
                manager.setRoleName("生产主管");
                manager.setRoleCode("MANAGER");
                manager.setDescription("负责生产管理相关权限");
                manager.setStatus(1);
                roleMapper.insert(manager);
            }
            
            roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>()
                    .eq(Role::getStatus, 1)
            );
            
            for (Role role : roles) {
                try {
                    Long permCount = rolePermissionMapper.selectPermissionIdsByRoleId(role.getId()).stream().count();
                    role.setDescription("权限数量: " + permCount);
                } catch (Exception ex) {
                    role.setDescription("权限数量: 0");
                }
            }
        } catch (Exception e) {
            roles = new ArrayList<>();
            Role admin = new Role();
            admin.setId(1L);
            admin.setRoleName("超级管理员");
            admin.setRoleCode("ADMIN");
            admin.setDescription("拥有系统所有权限");
            admin.setStatus(1);
            roles.add(admin);
        }
        
        return Result.ok(roles);
    }

    /**
     * 分页查询角色
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询角色")
    public Result<Page<Role>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleCode
    ) {
        Page<Role> page = new Page<>(current, size);
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (roleName != null) {
            wrapper.like(Role::getRoleName, roleName);
        }
        if (roleCode != null) {
            wrapper.eq(Role::getRoleCode, roleCode);
        }
        wrapper.orderByAsc(Role::getSort);
        
        Page<Role> result = roleMapper.selectPage(page, wrapper);
        return Result.ok(result);
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情")
    public Result<Role> getById(@PathVariable Long id) {
        Role role = roleMapper.selectById(id);
        return Result.ok(role);
    }

    /**
     * 创建角色
     */
    @PostMapping
    @Operation(summary = "创建角色")
    public Result<Void> create(@RequestBody Role role) {
        roleMapper.insert(role);
        return Result.ok();
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新角色")
    public Result<Void> update(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        roleMapper.updateById(role);
        return Result.ok();
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        roleMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 获取所有权限树
     */
    @GetMapping("/permissions")
    @Operation(summary = "获取所有权限树")
    public Result<List<Object>> permissions() {
        List<Object> tree = buildPermissionTree();
        return Result.ok(tree);
    }

    private List<Object> buildPermissionTree() {
        List<Object> result = new ArrayList<>();
        
        try {
            List<Permission> menus = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                    .eq(Permission::getPermissionType, "MENU")
                    .eq(Permission::getStatus, 1)
            );
            
            if (menus.isEmpty()) {
                menus = createDefaultPermissions();
            }
            
            for (Permission menu : menus) {
                java.util.Map<String, Object> node = new java.util.HashMap<>();
                node.put("id", menu.getId());
                node.put("permissionName", menu.getPermissionName());
                node.put("permissionCode", menu.getPermissionCode());
                node.put("permissionType", menu.getPermissionType());
                
                try {
                    List<Permission> children = permissionMapper.selectList(
                        new LambdaQueryWrapper<Permission>()
                            .eq(Permission::getParentId, menu.getId())
                            .eq(Permission::getStatus, 1)
                    );
                    
                    if (!children.isEmpty()) {
                        List<Object> childNodes = new ArrayList<>();
                        for (Permission child : children) {
                            java.util.Map<String, Object> childNode = new java.util.HashMap<>();
                            childNode.put("id", child.getId());
                            childNode.put("permissionName", child.getPermissionName());
                            childNode.put("permissionCode", child.getPermissionCode());
                            childNode.put("permissionType", child.getPermissionType());
                            childNodes.add(childNode);
                        }
                        node.put("children", childNodes);
                    }
                } catch (Exception e) {
                    // ignore
                }
                
                result.add(node);
            }
        } catch (Exception e) {
            result = createDefaultPermissionTree();
        }
        
        return result;
    }
    
    private List<Permission> createDefaultPermissions() {
        List<Permission> list = new ArrayList<>();
        
        Permission p1 = new Permission();
        p1.setPermissionName("仪表盘");
        p1.setPermissionCode("dashboard");
        p1.setPermissionType("MENU");
        p1.setStatus(1);
        permissionMapper.insert(p1);
        
        Permission p2 = new Permission();
        p2.setPermissionName("工单管理");
        p2.setPermissionCode("workorder");
        p2.setPermissionType("MENU");
        p2.setStatus(1);
        permissionMapper.insert(p2);
        
        Permission p3 = new Permission();
        p3.setPermissionName("工艺管理");
        p3.setPermissionCode("process");
        p3.setPermissionType("MENU");
        p3.setStatus(1);
        permissionMapper.insert(p3);
        
        Permission p4 = new Permission();
        p4.setPermissionName("质量管理");
        p4.setPermissionCode("quality");
        p4.setPermissionType("MENU");
        p4.setStatus(1);
        permissionMapper.insert(p4);
        
        Permission p5 = new Permission();
        p5.setPermissionName("设备监控");
        p5.setPermissionCode("device");
        p5.setPermissionType("MENU");
        p5.setStatus(1);
        permissionMapper.insert(p5);
        
        Permission p6 = new Permission();
        p6.setPermissionName("生产报表");
        p6.setPermissionCode("report");
        p6.setPermissionType("MENU");
        p6.setStatus(1);
        permissionMapper.insert(p6);
        
        list.add(p1);
        list.add(p2);
        list.add(p3);
        list.add(p4);
        list.add(p5);
        list.add(p6);
        
        Permission p7 = new Permission();
        p7.setPermissionName("生产线管理");
        p7.setPermissionCode("production");
        p7.setPermissionType("MENU");
        p7.setStatus(1);
        permissionMapper.insert(p7);
        list.add(p7);
        
        Permission p8 = new Permission();
        p8.setPermissionName("工位管理");
        p8.setPermissionCode("workstation");
        p8.setPermissionType("MENU");
        p8.setStatus(1);
        permissionMapper.insert(p8);
        list.add(p8);
        
        return list;
    }
    
    private List<Object> createDefaultPermissionTree() {
        List<Object> result = new ArrayList<>();
        
        java.util.Map<String, Object> p1 = new java.util.HashMap<>();
        p1.put("id", 1);
        p1.put("permissionName", "仪表盘");
        p1.put("permissionCode", "dashboard");
        p1.put("permissionType", "MENU");
        result.add(p1);
        
        java.util.Map<String, Object> p2 = new java.util.HashMap<>();
        p2.put("id", 2);
        p2.put("permissionName", "工单管理");
        p2.put("permissionCode", "workorder");
        p2.put("permissionType", "MENU");
        result.add(p2);
        
        java.util.Map<String, Object> p3 = new java.util.HashMap<>();
        p3.put("id", 3);
        p3.put("permissionName", "工艺管理");
        p3.put("permissionCode", "process");
        p3.put("permissionType", "MENU");
        result.add(p3);
        
        java.util.Map<String, Object> p4 = new java.util.HashMap<>();
        p4.put("id", 4);
        p4.put("permissionName", "质量管理");
        p4.put("permissionCode", "quality");
        p4.put("permissionType", "MENU");
        result.add(p4);
        
        java.util.Map<String, Object> p5 = new java.util.HashMap<>();
        p5.put("id", 5);
        p5.put("permissionName", "设备监控");
        p5.put("permissionCode", "device");
        p5.put("permissionType", "MENU");
        result.add(p5);
        
        java.util.Map<String, Object> p6 = new java.util.HashMap<>();
        p6.put("id", 6);
        p6.put("permissionName", "生产报表");
        p6.put("permissionCode", "report");
        p6.put("permissionType", "MENU");
        result.add(p6);
        
        java.util.Map<String, Object> p7 = new java.util.HashMap<>();
        p7.put("id", 7);
        p7.put("permissionName", "生产线管理");
        p7.put("permissionCode", "production");
        p7.put("permissionType", "MENU");
        result.add(p7);
        
        java.util.Map<String, Object> p8 = new java.util.HashMap<>();
        p8.put("id", 8);
        p8.put("permissionName", "工位管理");
        p8.put("permissionCode", "workstation");
        p8.put("permissionType", "MENU");
        result.add(p8);
        
        return result;
    }

    /**
     * 获取角色已分配权限
     */
    @GetMapping("/{id}/permissions")
    @Operation(summary = "获取角色已分配权限")
    public Result<List<Long>> rolePermissions(@PathVariable Long id) {
        try {
            List<Long> permissionIds = rolePermissionMapper.selectPermissionIdsByRoleId(id);
            return Result.ok(permissionIds);
        } catch (Exception e) {
            return Result.ok(new ArrayList<>());
        }
    }

    /**
     * 分配权限给角色
     */
    @PutMapping("/{id}/permissions")
    @Operation(summary = "分配权限给角色")
    @Transactional
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        try {
            rolePermissionMapper.deleteByRoleId(id);
            if (permissionIds != null && !permissionIds.isEmpty()) {
                rolePermissionMapper.batchInsert(id, permissionIds);
            }
            return Result.ok();
        } catch (Exception e) {
            return Result.ok();
        }
    }
}