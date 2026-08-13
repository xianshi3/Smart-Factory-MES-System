package com.mes.workorder.dto.planning;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 拖拽排产请求：把一个工单插入某设备的指定位置
 */
@Data
public class PlanningReorderDTO {
    /** 目标设备ID */
    @NotNull(message = "设备ID不能为空")
    private Long equipmentId;

    /** 该设备排产后的工单ID有序列表（含被拖入/拖出的工单） */
    @NotNull(message = "工单顺序列表不能为空")
    private List<Long> workOrderIds;
}