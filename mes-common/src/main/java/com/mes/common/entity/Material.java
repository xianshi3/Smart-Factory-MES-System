package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("material")
public class Material extends BaseEntity {

    private String materialCode;

    private String materialName;

    private String materialType;

    private String unit;

    private String spec;

    private BigDecimal defaultPrice;

    private Integer minStock;

    private Integer maxStock;

    private String status;

    private String description;
}
