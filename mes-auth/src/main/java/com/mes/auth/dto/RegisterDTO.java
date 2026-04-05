package com.mes.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册请求DTO
 */
@Data
public class RegisterDTO {
    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;
    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
    /** 真实姓名 */
    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
}
