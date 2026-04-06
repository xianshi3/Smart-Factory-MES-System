package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类
 * 所有业务实体的基类，包含通用字段
 */
@Data
public class BaseEntity implements Serializable {

    /**
     * 主键ID
     * 使用MyBatis-Plus的ASSIGN_ID策略自动生成
     */
    @TableId(type = IdType.ASSIGN_ID)
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
    @TableLogic
    private Integer deleted;

    /**
     * 删除时间
     */
    private LocalDateTime deletedTime;

    /**
     * 删除人ID
     */
    private Long deletedBy;

    /**
     * 乐观锁版本号
     * 用于并发控制，防止脏更新
     */
    @Version
    private Integer version;
}
