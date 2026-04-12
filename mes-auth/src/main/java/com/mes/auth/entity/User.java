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
    /** 昵称 */
    private String nickname;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 头像URL */
    private String avatar;
    /** 员工编号 */
    private String employeeNo;
    /** 部门 */
    private String department;
    /** 岗位 */
    private String position;
    /** 直接上级ID */
    private Long managerId;
    /** 入职日期 */
    private java.time.LocalDate hireDate;
    /** 状态：1-在职 0-离职 */
    private Integer status;
    /** 角色：ADMIN-管理员 MANAGER-主管 USER-普通用户 */
    private String role;
}
