package com.mes.workorder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateWorkOrderDTO {
    @NotBlank(message = "产品名称不能为空")
    @Size(max = 100, message = "产品名称过长")
    private String productName;

    @Size(max = 100, message = "产品型号过长")
    private String productModel;

    @NotNull(message = "计划数量不能为空")
    @Min(value = 1, message = "计划数量必须大于 0")
    @Max(value = 10000000, message = "计划数量过大")
    private Integer planQuantity;

    private Long workstationId;

    private Long processTemplateId;

    @Pattern(regexp = "^(LOW|MEDIUM|HIGH)$", message = "优先级只能是 LOW/MEDIUM/HIGH")
    private String priority;

    private LocalDateTime plannedStartTime;

    private LocalDateTime plannedEndTime;

    @Size(max = 500, message = "备注过长")
    private String remark;
}
