package com.mes.process.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

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
    private Long templateId;

    /** 参数名称 */
    private String paramName;

    /** 参数编码 */
    private String paramCode;

    /** 参数值 */
    private String paramValue;

    /** 最小值 */
    private Double minValue;

    /** 最大值 */
    private Double maxValue;

    /** 单位 */
    private String unit;

    /** 排序顺序 */
    private Integer sortOrder;

    /** 0-未删除 1-已删除 */
    private Integer deleted;
    /** 删除时间 */
    private LocalDateTime deletedTime;
    /** 删除人ID */
    private Long deletedBy;

    @TableLogic
    public Integer getDeleted() {
        return deleted;
    }
}
