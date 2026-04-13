package com.mes.workorder.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitReportDTO {
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;
    private Long deviceId;
    @NotNull(message = "报工数量不能为空")
    private Integer reportQuantity;
    private Integer qualifiedQuantity;
    private Integer defectiveQuantity;
    private String snStart;
    private String snEnd;
    private String remark;
}
