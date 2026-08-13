package com.mes.workorder.dto.planning;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排产看板 - 时间冲突
 */
@Data
public class PlanningConflictVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long workstationId;
    private String workstationName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long scheduleAId;
    private String orderNoA;
    private String stepNameA;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long scheduleBId;
    private String orderNoB;
    private String stepNameB;
    private LocalDateTime overlapStart;
    private LocalDateTime overlapEnd;
}
