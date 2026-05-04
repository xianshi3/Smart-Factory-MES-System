package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class Menu extends BaseEntity {
    /** 菜单名称 */
    @JsonProperty("menuName")
    private String menuName;
    /** 菜单编码 */
    @JsonProperty("menuCode")
    private String menuCode;
    /** 父菜单ID */
    @JsonProperty("parentId")
    private Long parentId;
    /** 路由路径 */
    @JsonProperty("path")
    private String path;
    /** 组件路径 */
    @JsonProperty("component")
    private String component;
    /** 图标 */
    @JsonProperty("icon")
    private String icon;
    /** 排序 */
    @JsonProperty("sort")
    private Integer sort;
    /** 是否可见: 1-是 0-否 */
    @JsonProperty("visible")
    private Integer visible;
    /** 状态: 1-启用 0-禁用 */
    @JsonProperty("status")
    private Integer status;
    
    /** 子菜单（不映射到数据库） */
    @TableField(exist = false)
    @JsonProperty("children")
    private List<Menu> children = new ArrayList<>();
}