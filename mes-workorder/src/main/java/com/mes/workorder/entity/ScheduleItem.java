package com.mes.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 排产明细实体（工序级）
 * 对应数据库表 wo_schedule
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wo_schedule")
public class ScheduleItem extends BaseEntity {

    /** 工单ID */
    @TableField("work_order_id")
    private Long workOrderId;

    /** 工序ID */
    @TableField("step_id")
    private Long stepId;

    /** 工序序号 */
    @TableField("step_no")
    private Integer stepNo;

    /** 工序名称 */
    @TableField("step_name")
    private String stepName;

    /** 设备ID */
    @TableField("workstation_id")
    private Long workstationId;

    /** 计划工时(分钟) */
    @TableField("duration_min")
    private Integer durationMin;

    /** 计划开始 */
    @TableField("planned_start")
    private LocalDateTime plannedStart;

    /** 计划结束 */
    @TableField("planned_end")
    private LocalDateTime plannedEnd;

    /** 同设备内顺序 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 排产状态: PLANNED-已排产 FROZEN-已冻结 RELEASED-已下发 HOLD-挂起 */
    private String status;

    /** 是否瓶颈工序 */
    private Integer bottleneck;

    /** 排产人 */
    @TableField("operator_id")
    private Long operatorId;
}
