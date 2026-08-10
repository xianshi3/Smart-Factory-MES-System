package com.mes.dashboard.service;

import com.mes.dashboard.entity.DeviceStatus;

import java.util.List;
import java.util.Map;

/**
 * 设备遥测时序服务（InfluxDB）
 */
public interface TelemetryService {

    /**
     * 写入设备遥测点
     */
    void writeTelemetry(DeviceStatus device);

    /**
     * 写入设备遥测点（Kafka 消费者场景，字段可能分散在 map 中）
     */
    void writeTelemetry(String deviceCode, String status, double temperature, double speed, double pressure, double power);

    /**
     * 查询设备历史遥测（按 interval 降采样聚合）
     * @param deviceCode 设备编码
     * @param hours 回溯小时数
     * @param interval 聚合间隔（秒），如 60
     * @return {times:[], temperature:[], speed:[], pressure:[], power:[]}
     */
    Map<String, Object> getDeviceHistory(String deviceCode, int hours, int interval);

    /**
     * 设备是否可用（InfluxDB 已配置）
     */
    boolean isEnabled();
}
