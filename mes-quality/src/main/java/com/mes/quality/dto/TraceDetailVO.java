package com.mes.quality.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 正向追溯详情VO（SN -> 工单 -> 工艺链路 -> 质量结果）
 */
@Data
public class TraceDetailVO {
    private String sn;
    private Long workOrderId;
    private String workOrderCode;
    private String qualityResult;
    private LocalDateTime qualityTime;
    private List<TraceStepVO> steps;
}
