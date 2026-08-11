package com.mes.process.dto;

import com.mes.process.entity.ProcessParameter;
import com.mes.process.entity.ProcessStep;
import com.mes.process.entity.ProcessTemplate;
import lombok.Data;

import java.util.List;

/**
 * 工艺模板详情VO（含参数与工序步骤）
 */
@Data
public class TemplateDetailVO {
    private ProcessTemplate template;
    private List<ProcessParameter> parameters;
    private List<ProcessStep> steps;
}
