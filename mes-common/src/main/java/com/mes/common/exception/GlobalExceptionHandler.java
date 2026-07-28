package com.mes.common.exception;

import com.mes.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 * 统一处理系统中的各类异常，返回标准化响应
 */
@Slf4j
@RestControllerAdvice
@Order(-1)
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e, HttpServletResponse response) {
        int code = e.getCode();
        response.setStatus(code == 401 ? HttpStatus.UNAUTHORIZED.value() : HttpStatus.BAD_REQUEST.value());
        if (code == 401) {
            log.warn("认证失败: {}", e.getMessage());
        } else {
            log.warn("业务异常: {}", e.getMessage());
        }
        return Result.fail(code, e.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid注解）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        StringBuilder errors = new StringBuilder();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.append(fieldName).append(": ").append(errorMessage).append("; ");
        });
        String message = errors.length() > 0 ? errors.toString() : "参数校验失败";
        log.error("参数校验失败: {}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        StringBuilder errors = new StringBuilder();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.append(fieldName).append(": ").append(errorMessage).append("; ");
        });
        String message = errors.length() > 0 ? errors.toString() : "参数绑定失败";
        log.error("参数绑定失败: {}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理请求体解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        String message = "请求体解析失败: " + e.getMessage();
        log.error(message, e);
        return Result.fail(400, message);
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数 '%s' 类型错误: 期望 %s，实际 %s", 
            e.getName(), 
            e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知",
            e.getValue());
        log.error("参数类型不匹配: {}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理缺少请求头异常
     */
    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleMissingRequestHeader(org.springframework.web.bind.MissingRequestHeaderException e) {
        log.error("缺少请求头: {}", e.getHeaderName());
        return Result.fail(401, "请先登录");
    }

    /**
     * 处理数据库唯一约束冲突
     */
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        String message = "数据已存在，请检查唯一约束字段";
        String causeMessage = e.getMostSpecificCause().getMessage();
        if (causeMessage != null && causeMessage.contains("Duplicate entry")) {
            message = extractDuplicateMessage(causeMessage);
        }
        log.error("唯一约束冲突: {}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理数据完整性异常
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleDataIntegrity(DataIntegrityViolationException e) {
        String sqlMessage = e.getMostSpecificCause().getMessage();
        String message = "数据操作失败";
        if (sqlMessage != null) {
            if (sqlMessage.contains("cannot be null") || sqlMessage.contains("doesn't have a default value")) {
                String field = extractFieldFromSQLMessage(sqlMessage);
                message = field + " 不能为空";
            } else if (sqlMessage.contains("Duplicate entry")) {
                message = extractDuplicateMessage(sqlMessage);
            } else if (sqlMessage.contains("Unknown column")) {
                message = "数据库字段不匹配: " + sqlMessage.substring(0, Math.min(sqlMessage.length(), 100));
            } else {
                message = "数据错误: " + sqlMessage.substring(0, Math.min(sqlMessage.length(), 100));
            }
        }
        log.error("数据完整性异常: {}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理404异常
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(NoHandlerFoundException e) {
        log.error("请求路径不存在: {}", e.getRequestURL());
        return Result.fail(404, "请求路径不存在: " + e.getRequestURL());
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
        
        String message = "系统内部错误";
        Throwable cause = e.getCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null && causeMessage.length() > 0) {
                message = causeMessage.length() > 100 ? causeMessage.substring(0, 100) : causeMessage;
            }
        }
        
        return Result.fail(500, message);
    }

    /**
     * 从SQL消息中提取重复数据信息
     */
    private String extractDuplicateMessage(String sqlMessage) {
        try {
            int start = sqlMessage.indexOf("'") + 1;
            int end = sqlMessage.lastIndexOf("'");
            if (start > 0 && end > start) {
                String value = sqlMessage.substring(start, end);
                return "数据 '" + value + "' 已存在";
            }
        } catch (Exception ignored) {}
        return "数据已存在，请检查唯一约束字段";
    }

    /**
     * 从SQL消息中提取字段名
     */
    private String extractFieldFromSQLMessage(String sqlMessage) {
        try {
            if (sqlMessage.contains("Field '")) {
                int start = sqlMessage.indexOf("Field '") + 7;
                int end = sqlMessage.indexOf("'", start);
                if (end > start) {
                    return sqlMessage.substring(start, end);
                }
            }
            if (sqlMessage.contains("column ")) {
                int start = sqlMessage.indexOf("column ") + 7;
                int end = sqlMessage.indexOf(" ", start);
                if (end > start) {
                    return sqlMessage.substring(start, end);
                }
            }
        } catch (Exception ignored) {}
        return "字段";
    }
}
