package com.mes.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 追溯记录实体类
 * @author MES
 * @description 存储产品追溯信息，支持正向追溯和反向追溯
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qms_traceability")
public class Traceability extends BaseEntity {

    /** 追溯ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 产品序列号SN */
    private String sn;

    /** 工单ID */
    private Long workOrderId;

    /** 工序步骤 */
    private String processStep;

    /** 物料批次号 */
    private String materialBatchNo;

    /** 设备ID */
    private Long equipmentId;

    /** 操作员ID */
    private Long operatorId;

    /** 参数快照 */
    private String paramSnapshot;

    /** 质量结果 */
    private String qualityResult;

    /** 创建时间 */
    private LocalDateTime createTime;
}
