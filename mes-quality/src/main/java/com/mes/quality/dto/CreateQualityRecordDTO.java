package com.mes.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建质检记录DTO
 * @author MES
 * @description 创建质检记录请求参数
 */
@Data
@Schema(description = "创建质检记录请求体")
public class CreateQualityRecordDTO {

    @Schema(description = "工单ID")
    private Long workOrderId;

    @Schema(description = "产品序列号SN")
    private String sn;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "检验类型: IPQC/FQC/OQC")
    private String checkType;

    @Schema(description = "检验结果: PASSED/FAILED/REWORK")
    private String checkResult;

    @Schema(description = "缺陷类型")
    private String defectType;

    @Schema(description = "缺陷描述")
    private String defectDesc;

    @Schema(description = "备注")
    private String remark;
}
