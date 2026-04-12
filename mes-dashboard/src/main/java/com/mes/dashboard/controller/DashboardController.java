package com.mes.dashboard.controller;

import com.mes.dashboard.dto.OeeQueryDTO;
import com.mes.dashboard.entity.DeviceStatus;
import com.mes.dashboard.entity.OeeData;
import com.mes.dashboard.entity.ProductionStats;
import com.mes.dashboard.service.DashboardService;
import com.mes.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 看板管理控制器
 * @author MES
 * @description 看板REST API接口
 */
@Tag(name = "看板管理", description = "生产看板相关接口")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 生产总览
     * @return 总览数据
     */
    @Operation(summary = "生产总览")
    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        return dashboardService.getOverview();
    }

    /**
     * 设备状态列表
     * @return 设备状态列表
     */
    @Operation(summary = "设备状态列表")
    @GetMapping("/devices")
    public List<DeviceStatus> getAllDeviceStatus() {
        return dashboardService.getAllDeviceStatus();
    }

    /**
     * 今日生产统计
     * @return 今日统计列表
     */
    @Operation(summary = "今日生产统计")
    @GetMapping("/production/today")
    public List<ProductionStats> getTodayStats() {
        return dashboardService.getTodayStats();
    }

    /**
     * OEE计算
     * @param dto 查询参数
     * @return OEE数据
     */
    @Operation(summary = "OEE计算")
    @GetMapping("/oee/calculate")
    public OeeData calculateOee(OeeQueryDTO dto) {
        return dashboardService.calculateOee(dto);
    }

    /**
     * 趋势数据
     * @param days 天数
     * @return 趋势数据
     */
    @Operation(summary = "趋势数据")
    @GetMapping("/trend")
    public Map<String, Object> getTrendData(@RequestParam(defaultValue = "7") int days) {
        return dashboardService.getTrendData(days);
    }

    /**
     * 获取告警设备列表
     * @return 告警设备列表
     */
    @Operation(summary = "告警设备列表")
    @GetMapping("/alarms")
    public List<DeviceStatus> getAlarmDevices() {
        return dashboardService.getAlarmDevices();
    }

    /**
     * 启动设备
     * @param deviceId 设备ID
     * @return 操作结果
     */
    @Operation(summary = "启动设备")
    @PostMapping("/device/{deviceId}/start")
    public Result<Void> startDevice(@PathVariable Long deviceId) {
        dashboardService.startDevice(deviceId);
        return Result.ok();
    }

    /**
     * 停止设备
     * @param deviceId 设备ID
     * @return 操作结果
     */
    @Operation(summary = "停止设备")
    @PostMapping("/device/{deviceId}/stop")
    public Result<Void> stopDevice(@PathVariable Long deviceId) {
        dashboardService.stopDevice(deviceId);
        return Result.ok();
    }

    /**
     * 设备控制（启动/停止）
     * @param deviceId 设备ID
     * @param action 操作类型 start/stop
     * @return 操作结果
     */
    @Operation(summary = "设备控制")
    @PostMapping("/device/{deviceId}/control")
    public Result<Void> controlDevice(@PathVariable Long deviceId, @RequestParam String action) {
        if ("start".equals(action)) {
            dashboardService.startDevice(deviceId);
        } else if ("stop".equals(action)) {
            dashboardService.stopDevice(deviceId);
        } else {
            return Result.fail("无效的操作: " + action);
        }
        return Result.ok();
    }

    /**
     * 生产报表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 报表数据
     */
    @Operation(summary = "生产报表")
    @GetMapping("/report/production")
    public Map<String, Object> getProductionReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return dashboardService.getProductionReport(startDate, endDate);
    }
}
