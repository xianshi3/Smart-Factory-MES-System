package com.mes.workorder.dto.planning;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排产看板 - 整体数据
 */
@Data
public class PlanningBoardVO {
    /** 看板时间窗口开始 */
    private LocalDateTime windowStart;
    /** 看板时间窗口结束 */
    private LocalDateTime windowEnd;
    /** 窗口内可用工作分钟（工作日历×班次） */
    private Long workMinutes;
    /** 设备分组（含负载率/瓶颈） */
    private List<PlanningEquipmentVO> equipment;
    /** 待排产工单（未分配设备） */
    private List<PlanningTaskVO> unassigned;
    /** 班次列表 */
    private List<PlanningShiftVO> shifts;
    /** 窗口内工作日历 */
    private List<PlanningCalendarVO> calendar;
    /** 冲突列表 */
    private List<PlanningConflictVO> conflicts;
    /** 最近变更日志 */
    private List<PlanningLogVO> logs;
}
