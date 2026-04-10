package com.mes.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 质检记录实体类
 * @author MES
 * @description 存储质检记录信息，包括工单、产品SN、检验结果等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qms_quality_record")
public class QualityRecord extends BaseEntity {

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 工单号 */
    private String workOrderNo;

    /** 产品序列号SN */
    private String sn;

    /** 设备ID */
    private Long deviceId;

    /** 工位ID */
    private Long workstationId;

    /** 操作员ID */
    private Long operatorId;

    /** 检验类型: IPQC/FQC/OQC */
    private String checkType;

    /** 检验结果: PASSED/FAILED/REWORK */
    private String checkResult;

    /** 缺陷类型 */
    private String defectType;

    /** 缺陷描述 */
    private String defectDesc;

    /** 检验时间 */
    private LocalDateTime checkTime;

    /** 备注 */
    private String remark;
}
