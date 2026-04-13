package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class Permission extends BaseEntity {
    /** 权限名称 */
    private String permissionName;
    /** 权限编码 */
    private String permissionCode;
    /** 权限类型: MENU/BUTTON/API */
    private String permissionType;
    /** 父权限ID */
    private Long parentId;
    /** 路由路径 */
    private String path;
    /** 图标 */
    private String icon;
    /** 排序 */
    private Integer sort;
    /** 状态: 1-启用 0-禁用 */
    private Integer status;
    
    /** 子权限（不映射到数据库） */
    @TableField(exist = false)
    private List<Permission> children = new ArrayList<>();
}