package com.mes.workorder.dto.planning;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排产看板 - 工序级甘特条目
 * ID 字段统一序列化为字符串，避免雪花ID(19位)超过JS Number精度丢失
 */
@Data
public class PlanningTaskVO {
    /** 排产明细ID（wo_schedule.id），未排产时为null */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long scheduleId;
    /** 工单ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String orderNo;
    private String productName;
    private String productModel;
    private String priority;
    /** 原始工单状态 */
    private String status;
    /** 看板状态：PENDING-待排产 READY-已排产待生产 RUNNING-运行中 COMPLETED-已完成 DELAYED-延误 */
    private String planStatus;
    /** 排产明细状态：PLANNED-已排产 FROZEN-已冻结 RELEASED-已下发 */
    private String scheduleStatus;
    private Integer planQuantity;
    private Integer completedQuantity;
    private Integer progress;
    /** 工序号 */
    private Integer stepNo;
    /** 工序名称 */
    private String stepName;
    /** 计划工时(分钟) */
    private Integer durationMin;
    /** 是否瓶颈工序 */
    private Boolean bottleneck;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long workstationId;
    private Integer sortOrder;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime actualStartTime;
    private String remark;
}
