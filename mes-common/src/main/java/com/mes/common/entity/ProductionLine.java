package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产线实体类
 * 对应数据库表 mes_production_line
 * @author MES
 * @since 2024
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_production_line")
public class ProductionLine extends BaseEntity {
    /** 生产线编码 */
    @JsonProperty("lineCode")
    private String lineCode;

    /** 生产线名称 */
    @JsonProperty("lineName")
    private String lineName;

    /** 状态: NORMAL-正常, STOPPED-停用 */
    private String status;
}