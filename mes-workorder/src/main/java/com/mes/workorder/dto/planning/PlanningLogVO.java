package com.mes.workorder.dto.planning;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排产看板 - 变更日志
 */
@Data
public class PlanningLogVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String orderNo;
    private String action;
    private String actionDesc;
    private LocalDateTime createTime;
}
