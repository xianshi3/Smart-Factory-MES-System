package com.mes.workorder.dto.planning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 排产顺序批量保存请求（拖拽后前端提交所有受影响设备的完整顺序）
 */
@Data
public class PlanningSaveOrderDTO {

    @Valid
    @NotNull(message = "设备顺序列表不能为空")
    private List<EquipmentOrder> groups;

    /** 拖回待排产池的工单ID（取消设备分配） */
    private List<Long> unassignedOrderIds;

    @Data
    public static class EquipmentOrder {
        /** 设备ID（工位ID） */
        private Long equipmentId;
        /** 该设备排产后的工单ID有序列表 */
        private List<Long> workOrderIds;
    }
}