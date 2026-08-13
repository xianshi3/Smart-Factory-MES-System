package com.mes.workorder.dto.planning;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排产看板 - 拖拽调整请求
 * 支持：整单移动/换设备/改时间；工序拉伸/缩短
 */
@Data
public class PlanningMoveRequestDTO {

    /** 工序明细ID（wo_schedule.id），null表示工单尚未排产需新建 */
    private Long scheduleId;

    /** 工单ID */
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 目标设备ID */
    @NotNull(message = "目标设备ID不能为空")
    private Long targetWorkstationId;

    /** 新的计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime newStart;

    /** 新的计划结束时间（工序拉伸/缩短时使用） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime newEnd;

    /** 新工序时长（分钟，无排产记录创建时使用） */
    private Integer durationMin;

    /** 是否强制（忽略冲突直接保存） */
    private Boolean force;
}
