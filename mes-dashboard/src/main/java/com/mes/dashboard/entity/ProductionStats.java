package com.mes.dashboard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生产统计实体类
 * @author MES
 * @description 存储生产统计数据，包括计划数、完成数、合格率、OEE等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dash_production_stats")
public class ProductionStats extends BaseEntity {

    /** 统计日期 */
    private String statDate;

    /** 工位ID */
    private Long workstationId;

    /** 工单ID */
    private Long workOrderId;

    /** 计划数量 */
    private Integer planQuantity;

    /** 完成数量 */
    private Integer completedQuantity;

    /** 合格数量 */
    private Integer qualifiedQuantity;

    /** 不良数量 */
    private Integer defectiveQuantity;

    /** OEE */
    private Double oeeRate;

    /** 可用率 */
    private Double availabilityRate;

    /** 性能率 */
    private Double performanceRate;

    /** 质量率 */
    private Double qualityRate;
}
