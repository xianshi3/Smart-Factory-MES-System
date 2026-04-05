package com.mes.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 追溯查询DTO
 * @author MES
 * @description 追溯查询条件
 */
@Data
@Schema(description = "追溯查询条件")
public class TraceQueryDTO {

    @Schema(description = "序列号SN - 用于正向追溯")
    private String sn;

    @Schema(description = "工单ID - 用于反向追溯")
    private Long workOrderId;
}
