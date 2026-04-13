package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
    private String menuName;
    /** 菜单编码 */
    private String menuCode;
    /** 父菜单ID */
    private Long parentId;
    /** 路由路径 */
    private String path;
    /** 组件路径 */
    private String component;
    /** 图标 */
    private String icon;
    /** 排序 */
    private Integer sort;
    /** 是否可见: 1-是 0-否 */
    private Integer visible;
    /** 状态: 1-启用 0-禁用 */
    private Integer status;
    
    /** 子菜单（不映射到数据库） */
    @TableField(exist = false)
    private List<Menu> children = new ArrayList<>();
}