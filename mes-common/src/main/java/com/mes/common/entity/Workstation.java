package com.mes.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工位实体类
 * 对应数据库表 mes_workstation
 * @author MES
 * @since 2024
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_workstation")
public class Workstation extends BaseEntity {
    /** 工位编码 */
    @JsonProperty("workstationCode")
    private String workstationCode;

    /** 工位名称 */
    @JsonProperty("workstationName")
    private String workstationName;

    /** 所属生产线ID */
    @JsonProperty("productionLineId")
    private Long productionLineId;

    /** 状态: IDLE-空闲, RUNNING-运行中, STOPPED-停用 */
    private String status;
}