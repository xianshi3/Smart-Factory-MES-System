package com.mes.process.dto;

import lombok.Data;

import java.util.Map;

/**
 * 参数校验DTO
 * @author MES
 * @description 工艺参数校验请求参数
 */
@Data
public class ParameterCheckDTO {
    /** 模板ID */
    private Long templateId;
    /** 参数值映射 */
    private Map<String, Double> paramValues;
}
