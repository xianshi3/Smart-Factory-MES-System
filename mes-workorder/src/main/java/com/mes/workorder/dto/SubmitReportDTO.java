package com.mes.workorder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitReportDTO {
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    private Long deviceId;

    @NotNull(message = "报工数量不能为空")
    @Min(value = 1, message = "报工数量必须大于 0")
    @Max(value = 10000000, message = "报工数量过大")
    private Integer reportQuantity;

    @Min(value = 0, message = "合格数量不能为负")
    private Integer qualifiedQuantity;

    @Min(value = 0, message = "不合格数量不能为负")
    private Integer defectiveQuantity;

    @Size(max = 100, message = "序列号起始过长")
    private String snStart;

    @Size(max = 100, message = "序列号结束过长")
    private String snEnd;

    @Size(max = 500, message = "备注过长")
    private String remark;
}
