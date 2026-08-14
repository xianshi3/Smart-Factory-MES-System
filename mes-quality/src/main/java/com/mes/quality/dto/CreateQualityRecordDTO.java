package com.mes.quality.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @Schema(description = "工单号")
    @Size(max = 50, message = "工单号过长")
    private String workOrderNo;

    @Schema(description = "产品序列号SN")
    @Size(max = 100, message = "序列号过长")
    private String sn;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "工位ID")
    private Long workstationId;

    @Schema(description = "操作员ID")
    private Long operatorId;

    @Schema(description = "检验类型: IPQC/FQC/OQC")
    @NotBlank(message = "检验类型不能为空")
    @Pattern(regexp = "^(IPQC|FQC|OQC|巡检|首检|终检)$", message = "检验类型只能是 IPQC/FQC/OQC")
    private String checkType;

    @Schema(description = "检验结果: PASSED/FAILED/REWORK")
    @NotBlank(message = "检验结果不能为空")
    @Pattern(regexp = "^(PASSED|FAILED|REWORK|PASS|FAIL)$", message = "检验结果只能是 PASSED/FAILED/REWORK")
    private String checkResult;

    @Schema(description = "缺陷类型")
    @Size(max = 50, message = "缺陷类型过长")
    private String defectType;

    @Schema(description = "缺陷描述")
    @Size(max = 500, message = "缺陷描述过长")
    private String defectDesc;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注过长")
    private String remark;
}
