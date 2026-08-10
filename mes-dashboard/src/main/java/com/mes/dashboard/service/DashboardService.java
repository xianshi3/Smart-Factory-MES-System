package com.mes.dashboard.service;

import com.mes.dashboard.dto.OeeQueryDTO;
import com.mes.dashboard.entity.DeviceStatus;
import com.mes.dashboard.entity.OeeData;
import com.mes.dashboard.entity.ProductionStats;

import java.util.List;
import java.util.Map;

/**
 * 看板管理服务接口
 * @author MES
 * @description 看板相关业务逻辑
 */
public interface DashboardService {

    /**
     * 获取生产总览数据
     * @return 总览数据
     */
    Map<String, Object> getOverview();

    /**
     * 获取所有设备状态
     * @return 设备状态列表
     */
    List<DeviceStatus> getAllDeviceStatus();

    /**
     * 获取今日生产统计
     * @return 今日统计列表
     */
    List<ProductionStats> getTodayStats();

    /**
     * 计算OEE
     * @param dto 查询参数
     * @return OEE数据
     */
    OeeData calculateOee(OeeQueryDTO dto);

    /**
     * 获取趋势数据
     * @param days 天数
     * @return 趋势数据
     */
    Map<String, Object> getTrendData(int days);

    /**
     * 保存设备数据
     * @param data 设备数据
     */
    void saveDeviceData(DeviceStatus data);

    void createDevice(DeviceStatus device);

    void deleteDevice(Long deviceId);

    void deleteDeviceByCode(String deviceCode);

    void deleteAllDevices();

    void updateDevice(DeviceStatus device);

    /**
     * 获取告警设备列表
     * @return 告警设备列表
     */
    List<DeviceStatus> getAlarmDevices();

    /**
     * 启动设备
     * @param deviceId 设备ID
     */
    void startDevice(Long deviceId);

    /**
     * 停止设备
     * @param deviceId 设备ID
     */
    void stopDevice(Long deviceId);

    /**
     * 获取生产报表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 报表数据
     */
    Map<String, Object> getProductionReport(String startDate, String endDate);

    /**
     * 获取设备历史遥测（InfluxDB 时序）
     * @param deviceCode 设备编码
     * @param hours 回溯小时数
     * @param interval 聚合间隔（秒）
     * @return 时间序列 {times, temperature, speed, pressure, power, enabled}
     */
    Map<String, Object> getDeviceHistory(String deviceCode, int hours, int interval);
}
