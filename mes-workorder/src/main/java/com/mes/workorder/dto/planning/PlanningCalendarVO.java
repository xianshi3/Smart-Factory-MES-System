package com.mes.workorder.dto.planning;

import lombok.Data;

import java.time.LocalDate;

/**
 * 排产看板 - 工作日历条目
 */
@Data
public class PlanningCalendarVO {
    private LocalDate workDate;
    /** 是否工作日 */
    private Boolean workday;
    private String remark;
}
