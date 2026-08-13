package com.mes.workorder.dto.planning;

import lombok.Data;

import java.time.LocalTime;

/**
 * 排产看板 - 班次
 */
@Data
public class PlanningShiftVO {
    private String shiftCode;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean work;
}
