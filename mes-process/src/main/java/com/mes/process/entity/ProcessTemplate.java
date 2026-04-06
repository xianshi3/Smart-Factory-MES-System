package com.mes.process.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工艺模板实体类
 * @author MES
 * @description 存储工艺模板信息，包括模板名称、编码、产品型号、版本等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("proc_template")
public class ProcessTemplate extends BaseEntity {
    /** 模板名称 */
    private String templateName;

    /** 模板编码 */
    private String templateCode;

    /** 产品型号 */
    private String productModel;

    /** 版本号 */
    private Integer version;

    /** 状态: DRAFT/PUBLISHED */
    private String status;

    /** 描述信息 */
    private String description;
}
