package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bom_item")
public class BomItem extends BaseEntity {

    private Long bomId;

    private Long materialId;

    private BigDecimal quantity;

    private String unit;

    private BigDecimal scrapRate;

    private Integer sequence;

    private String remark;
}
