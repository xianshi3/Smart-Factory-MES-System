package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_user 表只读映射（供权限校验使用，仅取认证所需字段）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUserAuth extends BaseEntity {
    /** 角色ID（关联 sys_role.id） */
    @TableField("role_id")
    @JsonIgnore
    private Long roleId;

    /** 角色编码：ADMIN/MANAGER/USER */
    @TableField("role")
    @JsonIgnore
    private String role;
}