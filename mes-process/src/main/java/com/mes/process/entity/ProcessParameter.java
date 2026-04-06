package com.mes.process.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工艺参数实体类
 * @author MES
 * @description 存储工艺模板中的参数配置，包括参数名、值、上下限等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("proc_parameter")
public class ProcessParameter extends BaseEntity {
    /** 参数ID */
    private Long id;

    /** 模板ID */
    @TableField("template_id")
    private Long templateId;

    /** 参数名称 */
    @TableField("param_name")
    private String paramName;

    /** 参数编码 */
    @TableField("param_code")
    private String paramCode;

    /** 参数值 */
    @TableField("param_value")
    private String paramValue;

    /** 最小值 */
    @TableField("min_value")
    private Double minValue;

    /** 最大值 */
    @TableField("max_value")
    private Double maxValue;

    /** 单位 */
    private String unit;

    /** 排序顺序 */
    @TableField("sort_order")
    private Integer sortOrder;
}
