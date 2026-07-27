package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inventory_transaction")
public class InventoryTransaction extends BaseEntity {

    private String transactionNo;

    private Long materialId;

    private String transactionType;

    private BigDecimal quantity;

    private BigDecimal balanceAfter;

    private String batchNo;

    private String referenceType;

    private Long referenceId;

    private String remark;

    private String createBy;
}
