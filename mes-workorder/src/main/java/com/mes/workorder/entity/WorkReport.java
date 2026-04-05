package com.mes.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wo_work_report")
public class WorkReport extends BaseEntity {
    private Long workOrderId;
    private Long deviceId;
    private Long operatorId;
    private Integer reportQuantity;
    private Integer qualifiedQuantity;
    private Integer defectiveQuantity;
    private LocalDateTime reportTime;
    private String snStart;
    private String snEnd;
    private String remark;
}
