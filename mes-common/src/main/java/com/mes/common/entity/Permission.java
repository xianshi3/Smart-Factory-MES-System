package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("permissionName")
    private String permissionName;
    /** 权限编码 */
    @JsonProperty("permissionCode")
    private String permissionCode;
    /** 权限类型: MENU/BUTTON/API */
    @JsonProperty("permissionType")
    private String permissionType;
    /** 父权限ID */
    @JsonProperty("parentId")
    private Long parentId;
    /** 路由路径 */
    @JsonProperty("path")
    private String path;
    /** 图标 */
    @JsonProperty("icon")
    private String icon;
    /** 排序 */
    @JsonProperty("sort")
    private Integer sort;
    /** 状态: 1-启用 0-禁用 */
    @JsonProperty("status")
    private Integer status;
    
    /** 子权限（不映射到数据库） */
    @TableField(exist = false)
    @JsonProperty("children")
    private List<Permission> children = new ArrayList<>();
}