package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bom")
public class Bom extends BaseEntity {

    private String bomCode;

    private String bomName;

    private Long productId;

    private BigDecimal productQuantity;

    private String version;

    private String status;

    private String description;

    private String createBy;

    private String updateBy;
}
