package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inventory")
public class Inventory extends BaseEntity {

    private Long materialId;

    private String warehouse;

    private String batchNo;

    private BigDecimal quantity;

    private BigDecimal lockedQuantity;

    /** 数据库 GENERATED 列 (quantity - locked_quantity)，不可写入 */
    @TableField(exist = false)
    private BigDecimal availableQuantity;

    private LocalDateTime lastTransactionTime;
}
