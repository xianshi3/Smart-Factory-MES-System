package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base Entity
 * Base class for all business entities with common fields
 */
@Data
public class BaseEntity implements Serializable {

    /**
     * 主键ID
     * 使用MyBatis-Plus的ASSIGN_ID策略自动生成
     * 使用ToStringSerializer将Long序列化为String，避免JavaScript精度丢失
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 创建时间
     * 自动填充，插入时自动设置
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 自动填充，插入和更新时自动设置
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志
     * 0-未删除 1-已删除
     */
    //@TableLogic  // 禁用逻辑删除，使用物理删除
    private Integer deleted;

    /**
     * 删除时间
     */
    @TableField(exist = false)
    private LocalDateTime deletedTime;

    /**
     * 删除人ID
     */
    @TableField(exist = false)
    private Long deletedBy;
}
