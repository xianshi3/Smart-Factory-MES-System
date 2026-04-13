package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 角色实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class Role extends BaseEntity {
    /** 角色名称 */
    private String roleName;
    /** 角色编码 */
    private String roleCode;
    /** 角色描述 */
    private String description;
    /** 状态: 1-启用 0-禁用 */
    private Integer status;
    /** 排序 */
    private Integer sort;
}