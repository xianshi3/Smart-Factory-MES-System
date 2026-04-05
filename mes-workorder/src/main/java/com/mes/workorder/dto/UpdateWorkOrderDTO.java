package com.mes.workorder.dto;

import lombok.Data;

@Data
public class UpdateWorkOrderDTO {
    private String status;
    private String priority;
    private String remark;
}
