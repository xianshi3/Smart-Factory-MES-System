package com.mes.workorder.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateWorkOrderDTO {
    private String status;
    private String priority;
    private String remark;
    private String productName;
    private String productModel;
    private Integer planQuantity;
    private Long workstationId;
    private Long processTemplateId;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
}
