package com.mes.workorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateWorkOrderDTO {
    private String productName;
    private String productModel;
    private Integer planQuantity;
    private Long workstationId;
    private Long processTemplateId;
    private String priority;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private String remark;
}
