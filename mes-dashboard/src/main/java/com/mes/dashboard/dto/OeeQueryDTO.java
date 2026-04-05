package com.mes.dashboard.dto;

import lombok.Data;

/**
 * OEE查询DTO
 * @author MES
 * @description OEE计算查询参数
 */
@Data
public class OeeQueryDTO {

    /** 设备ID */
    private Long deviceId;

    /** 开始日期 */
    private String startDate;

    /** 结束日期 */
    private String endDate;
}
