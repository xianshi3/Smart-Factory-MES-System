package com.mes.workorder.dto.planning;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;

/**
 * 排产看板 - 设备（工位）分组及负载
 */
@Data
public class PlanningEquipmentVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String workstationCode;
    private String workstationName;
    /** 设备状态：IDLE/RUNNING/STOPPED */
    private String status;
    /** 每小时产能(件) */
    private Integer capacityPerHour;
    /** 是否瓶颈设备 */
    private Boolean bottleneck;
    /** 负载率百分比 0-100（基于窗口内可用工时） */
    private Integer loadRate;
    /** 该设备排产工序数 */
    private Integer taskCount;
    /** 该设备的工序（按planned_start排序） */
    private List<PlanningTaskVO> tasks;
}
