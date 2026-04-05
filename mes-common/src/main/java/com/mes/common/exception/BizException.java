package com.mes.common.exception;

import com.mes.common.result.Result;
import lombok.Getter;

/**
 * 业务异常类
 * 用于抛出业务相关的异常，包含错误码和错误信息
 */
@Getter
public class BizException extends RuntimeException {
    /** 错误码 */
    private final int code;

    /**
     * 构造默认业务异常（错误码500）
     */
    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 构造自定义错误码的业务异常
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 根据错误码枚举构造业务异常
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
