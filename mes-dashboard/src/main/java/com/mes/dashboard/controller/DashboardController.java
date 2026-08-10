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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "看板管理", description = "生产看板相关接口")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "设备模拟数据")
    @PostMapping("/device/simulate")
    public Result<Void> simulateDevice(@RequestBody DeviceStatus device) {
        dashboardService.saveDeviceData(device);
        return Result.ok();
    }

    @Operation(summary = "创建设备")
    @PostMapping("/device")
    public Result<DeviceStatus> createDevice(@RequestBody DeviceStatus device) {
        dashboardService.createDevice(device);
        return Result.ok(device);
    }

    @Operation(summary = "批量创建设备")
    @PostMapping("/device/batch")
    public Result<Integer> createDevicesBatch(@RequestBody List<DeviceStatus> devices) {
        int created = 0;
        if (devices != null) {
            for (DeviceStatus device : devices) {
                try {
                    dashboardService.createDevice(device);
                    created++;
                } catch (Exception e) {
                    // 跳过已存在/异常设备，继续批量创建
                    log.warn("Batch create skipped device {}: {}", device.getDeviceCode(), e.getMessage());
                }
            }
        }
        return Result.ok(created);
    }

    @Operation(summary = "删除设备")
    @DeleteMapping("/device/{deviceCode}")
    public Result<Void> deleteDevice(@PathVariable String deviceCode) {
        dashboardService.deleteDeviceByCode(deviceCode);
        return Result.ok();
    }

    @Operation(summary = "清空所有设备")
    @DeleteMapping("/devices/all")
    public Result<Void> deleteAllDevices() {
        dashboardService.deleteAllDevices();
        return Result.ok();
    }

    @Operation(summary = "更新设备")
    @PutMapping("/device")
    public Result<DeviceStatus> updateDevice(@RequestBody DeviceStatus device) {
        dashboardService.updateDevice(device);
        return Result.ok(device);
    }

    @Operation(summary = "生产总览")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        return Result.ok(dashboardService.getOverview());
    }

    @Operation(summary = "设备状态列表")
    @GetMapping("/devices")
    public Result<List<DeviceStatus>> getAllDeviceStatus() {
        return Result.ok(dashboardService.getAllDeviceStatus());
    }

    @Operation(summary = "今日生产统计")
    @GetMapping("/production/today")
    public Result<List<ProductionStats>> getTodayStats() {
        return Result.ok(dashboardService.getTodayStats());
    }

    @Operation(summary = "OEE计算")
    @GetMapping("/oee/calculate")
    public Result<OeeData> calculateOee(OeeQueryDTO dto) {
        return Result.ok(dashboardService.calculateOee(dto));
    }

    @Operation(summary = "趋势数据")
    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrendData(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(dashboardService.getTrendData(days));
    }

    @Operation(summary = "告警设备列表")
    @GetMapping("/alarms")
    public Result<List<DeviceStatus>> getAlarmDevices() {
        return Result.ok(dashboardService.getAlarmDevices());
    }

    @Operation(summary = "启动设备")
    @PostMapping("/device/{deviceId}/start")
    public Result<Void> startDevice(@PathVariable Long deviceId) {
        dashboardService.startDevice(deviceId);
        return Result.ok();
    }

    @Operation(summary = "停止设备")
    @PostMapping("/device/{deviceId}/stop")
    public Result<Void> stopDevice(@PathVariable Long deviceId) {
        dashboardService.stopDevice(deviceId);
        return Result.ok();
    }

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

    @Operation(summary = "设备历史遥测")
    @GetMapping("/device/{deviceCode}/history")
    public Result<Map<String, Object>> getDeviceHistory(
            @PathVariable String deviceCode,
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "60") int interval) {
        return Result.ok(dashboardService.getDeviceHistory(deviceCode, hours, interval));
    }

    @Operation(summary = "生产报表")
    @GetMapping("/report/production")
    public Result<Map<String, Object>> getProductionReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(dashboardService.getProductionReport(startDate, endDate));
    }
}
