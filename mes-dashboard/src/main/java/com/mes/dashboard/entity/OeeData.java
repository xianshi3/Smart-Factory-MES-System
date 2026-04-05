package com.mes.dashboard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OEE数据实体类
 * @author MES
 * @description 存储OEE计算数据，包括可用时间、运行时间、良品数等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dash_oee_data")
public class OeeData extends BaseEntity {

    /** 设备ID */
    private Long deviceId;

    /** 日期小时 */
    private String dateHour;

    /** 可用时间(秒) */
    private Long availableTime;

    /** 运行时间(秒) */
    private Long runTime;

    /** 停机时间(秒) */
    private Long downtime;

    /** 总产量 */
    private Integer totalProducts;

    /** 良品数 */
    private Integer goodProducts;

    /** 不良品数 */
    private Integer defectiveProducts;

    /** 理想周期时间 */
    private Double idealCycleTime;

    /** OEE */
    private Double oee;

    /** 可用率 */
    private Double availability;

    /** 性能率 */
    private Double performance;

    /** 质量率 */
    private Double quality;
}
