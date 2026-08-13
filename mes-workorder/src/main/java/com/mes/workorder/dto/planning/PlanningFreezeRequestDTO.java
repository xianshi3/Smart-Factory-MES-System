package com.mes.workorder.dto.planning;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 排产看板 - 冻结/释放请求（设备行或工单）
 */
@Data
public class PlanningFreezeRequestDTO {

    /** 设备ID（冻结该设备全部工序） */
    private Long workstationId;

    /** 工单ID（冻结该工单全部工序） */
    private Long workOrderId;

    /** 排产明细ID列表（精确冻结） */
    private java.util.List<Long> scheduleIds;

    @NotNull(message = "请指定冻结范围")
    private String scope;
}
