package com.mes.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单实体类
 * 对应数据库表 wo_work_order
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wo_work_order")
public class WorkOrder extends BaseEntity {
    /** 工单编号 */
    @TableField("order_no")
    private String orderNo;

    /** 产品名称 */
    @TableField("product_name")
    private String productName;

    /** 产品型号 */
    @TableField("product_model")
    private String productModel;

    /** 计划数量 */
    @TableField("plan_quantity")
    private Integer planQuantity;

    /** 已完成数量 */
    @TableField("completed_quantity")
    private Integer completedQuantity;

    /** 工单状态 */
    private String status;

    /** 工作站ID */
    @TableField("workstation_id")
    private Long workstationId;

    /** 工艺模板ID */
    @TableField("process_template_id")
    private Long processTemplateId;

    /** 优先级：LOW/MEDIUM/HIGH */
    private String priority;

    /** 排产顺序（同设备内，0表示未排产） */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 计划开始时间 */
    @TableField("planned_start_time")
    private LocalDateTime plannedStartTime;

    /** 计划结束时间 */
    @TableField("planned_end_time")
    private LocalDateTime plannedEndTime;

    /** 实际开始时间 */
    @TableField("actual_start_time")
    private LocalDateTime actualStartTime;

    /** 实际结束时间 */
    @TableField("actual_end_time")
    private LocalDateTime actualEndTime;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    @TableField("create_by")
    private Long createBy;

    /** 下发人ID */
    @TableField("issue_by")
    private Long issueBy;
}
