package com.mes.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateWorkOrderDTO {
    @NotBlank(message = "产品名称不能为空")
    private String productName;
    @NotBlank(message = "产品型号不能为空")
    private String productModel;
    @NotNull(message = "计划数量不能为空")
    private Integer planQuantity;
    @NotNull(message = "工位ID不能为空")
    private Long workstationId;
    @NotNull(message = "工艺模板ID不能为空")
    private Long processTemplateId;
    private String priority;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private String remark;
}
