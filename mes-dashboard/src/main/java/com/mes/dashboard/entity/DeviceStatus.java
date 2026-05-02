package com.mes.dashboard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 设备状态实体类
 * @author MES
 * @description 存储设备状态信息，包括设备编码、名称、状态、心跳等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dash_device_status")
public class DeviceStatus extends BaseEntity {

    /** 设备编码 */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 设备类型 */
    private String deviceType;

    /** 状态: ONLINE/OFFLINE/ALARM */
    private String status;

    /** 温度 */
    private Double temperature;

    /** 速度 */
    private Double speed;

    /** 最后心跳时间 */
    private LocalDateTime lastHeartbeat;

    /** 工位ID */
    private Long workstationId;

    /** 生产线ID */
    private Long productionLineId;
}
