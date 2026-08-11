package com.mes.quality.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 追溯步骤VO（正向追溯链路中的单个工序节点）
 */
@Data
public class TraceStepVO {
    private Long id;
    private String processStep;
    private String materialBatchNo;
    private Long equipmentId;
    private Long operatorId;
    private String paramSnapshot;
    private LocalDateTime createTime;
}
