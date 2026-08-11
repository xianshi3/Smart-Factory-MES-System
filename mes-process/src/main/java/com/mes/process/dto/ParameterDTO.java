package com.mes.process.dto;

import lombok.Data;

/**
 * 工艺参数DTO
 */
@Data
public class ParameterDTO {
    private Long id;
    private Long templateId;
    private String paramName;
    private String paramCode;
    private String paramValue;
    private Double minValue;
    private Double maxValue;
    private String unit;
    private Integer sortOrder;
}
