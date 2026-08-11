package com.mes.process.dto;

import lombok.Data;

/**
 * 工艺工序步骤DTO
 */
@Data
public class StepDTO {
    private Long id;
    private Long templateId;
    private Integer stepNo;
    private String stepName;
    private String stepDesc;
    private Integer durationMin;
    private Integer sequence;
}
