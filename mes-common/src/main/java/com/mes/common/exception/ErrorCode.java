package com.mes.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 * 定义系统统一的错误码和错误信息
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 通用错误码
    SUCCESS(200, "操作成功"),
    SYSTEM_ERROR(500, "系统内部错误"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),

    // 工单模块错误码 (1xxx)
    WORKORDER_NOT_FOUND(1001, "工单不存在"),
    WORKORDER_STATUS_ERROR(1002, "工单状态异常"),

    // 设备模块错误码 (2xxx)
    DEVICE_NOT_FOUND(2001, "设备不存在"),
    DEVICE_OFFLINE(2002, "设备离线"),

    // 工艺模块错误码 (3xxx)
    PROCESS_NOT_FOUND(3001, "工艺模板不存在"),

    // 质量模块错误码 (4xxx)
    QUALITY_RECORD_NOT_FOUND(4001, "质检记录不存在"),

    // 用户模块错误码 (5xxx)
    USER_NOT_FOUND(5001, "用户不存在"),
    USER_PASSWORD_ERROR(5002, "密码错误"),
    TOKEN_EXPIRED(5003, "Token已过期"),
    TOKEN_INVALID(5004, "Token无效");

    /** 错误码 */
    private final int code;
    /** 错误信息 */
    private final String message;
}
