package com.mes.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 * 对应数据库表 sys_user
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {
    /** 用户名（登录账号） */
    private String username;
    /** 密码（加密存储） */
    private String password;
    /** 真实姓名 */
    private String realName;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 状态：1-启用 0-禁用 */
    private Integer status;
    /** 角色：ADMIN-管理员 USER-普通用户 */
    private String role;
}
