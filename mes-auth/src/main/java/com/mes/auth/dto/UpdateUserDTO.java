package com.mes.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户信息DTO
 */
@Data
@Schema(description = "更新用户信息请求")
public class UpdateUserDTO {

    @Schema(description = "真实姓名")
    @Size(max = 50, message = "真实姓名长度不能超过50")
    private String realName;

    @Schema(description = "昵称")
    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;

    @Schema(description = "手机号")
    @Size(max = 20, message = "手机号长度不能超过20")
    private String phone;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "部门")
    @Size(max = 50, message = "部门长度不能超过50")
    private String department;

    @Schema(description = "岗位")
    @Size(max = 50, message = "岗位长度不能超过50")
    private String position;
}