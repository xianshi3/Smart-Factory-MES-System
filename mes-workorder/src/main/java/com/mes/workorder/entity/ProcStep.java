package com.mes.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工艺步骤实体（只读用于排产）
 * 对应数据库表 proc_step
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("proc_step")
public class ProcStep extends BaseEntity {

    /** 工艺模板ID */
    @TableField("template_id")
    private Long templateId;

    /** 工序序号 */
    @TableField("step_no")
    private Integer stepNo;

    /** 工序名称 */
    @TableField("step_name")
    private String stepName;

    /** 工序描述 */
    @TableField("step_desc")
    private String stepDesc;

    /** 标准工时(分钟) */
    @TableField("duration_min")
    private Integer durationMin;

    /** 顺序 */
    private Integer sequence;
}
