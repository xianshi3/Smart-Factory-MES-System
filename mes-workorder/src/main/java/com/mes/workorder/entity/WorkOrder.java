package com.mes.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
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
    private String orderNo;
    /** 产品名称 */
    private String productName;
    /** 产品型号 */
    private String productModel;
    /** 计划数量 */
    private Integer planQuantity;
    /** 已完成数量 */
    private Integer completedQuantity;
    /** 工单状态 */
    private String status;
    /** 工作站ID */
    private Long workstationId;
    /** 工艺模板ID */
    private Long processTemplateId;
    /** 优先级：LOW/MEDIUM/HIGH */
    private String priority;
    /** 计划开始时间 */
    private LocalDateTime plannedStartTime;
    /** 计划结束时间 */
    private LocalDateTime plannedEndTime;
    /** 实际开始时间 */
    private LocalDateTime actualStartTime;
    /** 实际结束时间 */
    private LocalDateTime actualEndTime;
    /** 备注 */
    private String remark;
    /** 创建人ID */
    private Long createBy;
    /** 下发人ID */
    private Long issueBy;
    /** 0-未删除 1-已删除 */
    @TableLogic
    private Integer deleted;
    /** 删除时间 */
    private LocalDateTime deletedTime;
    /** 删除人ID */
    private Long deletedBy;
}
