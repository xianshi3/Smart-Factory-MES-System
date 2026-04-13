package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色权限关联实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_permission")
public class RolePermission extends BaseEntity {
    /** 角色ID */
    private Long roleId;
    /** 权限ID */
    private Long permissionId;
    /** 排序 */
    private Integer sort;
}