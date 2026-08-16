package com.mes.dashboard.controller;

import com.mes.dashboard.dto.OeeQueryDTO;
import com.mes.dashboard.entity.DeviceStatus;
import com.mes.dashboard.entity.OeeData;
import com.mes.dashboard.entity.ProductionStats;
import com.mes.dashboard.service.DashboardService;
import com.mes.common.result.Result;
import com.mes.common.security.RequirePermission;
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
    @RequirePermission("device:control")
    public Result<Void> simulateDevice(@RequestBody DeviceStatus device) {
        dashboardService.saveDeviceData(device);
        return Result.ok();
    }

    @Operation(summary = "创建设备")
    @PostMapping("/device")
    @RequirePermission("device:control")
    public Result<DeviceStatus> createDevice(@RequestBody DeviceStatus device) {
        dashboardService.createDevice(device);
        return Result.ok(device);
    }

    @Operation(summary = "批量创建设备")
    @PostMapping("/device/batch")
    @RequirePermission("device:control")
    public Result<Map<String, Object>> createDevicesBatch(@RequestBody List<DeviceStatus> devices) {
        int created = 0;
        int skipped = 0;
        List<String> errors = new java.util.ArrayList<>();
        if (devices != null) {
            for (DeviceStatus device : devices) {
                try {
                    dashboardService.createDevice(device);
                    created++;
                } catch (Exception e) {
                    skipped++;
                    String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                    // 截断过长 SQL 详情，避免接口暴露完整 SQL
                    if (reason.length() > 120) reason = reason.substring(0, 120) + "...";
                    errors.add((device.getDeviceCode() != null ? device.getDeviceCode() : "?") + ": " + reason);
                    log.warn("Batch create skipped device {}: {}", device.getDeviceCode(), reason);
                }
            }
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return Result.ok(result);
    }

    @Operation(summary = "删除设备")
    @DeleteMapping("/device/{deviceCode}")
    @RequirePermission("device:control")
    public Result<Void> deleteDevice(@PathVariable String deviceCode) {
        dashboardService.deleteDeviceByCode(deviceCode);
        return Result.ok();
    }

    @Operation(summary = "清空所有设备")
    @DeleteMapping("/devices/all")
    @RequirePermission("device:control")
    public Result<Void> deleteAllDevices() {
        dashboardService.deleteAllDevices();
        return Result.ok();
    }

    @Operation(summary = "更新设备")
    @PutMapping("/device")
    @RequirePermission("device:control")
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
        // 限制查询窗口，防止超大 days 导致 N+1 循环查询
        int safeDays = Math.min(Math.max(days, 1), 366);
        return Result.ok(dashboardService.getTrendData(safeDays));
    }

    @Operation(summary = "告警设备列表")
    @GetMapping("/alarms")
    public Result<List<DeviceStatus>> getAlarmDevices() {
        return Result.ok(dashboardService.getAlarmDevices());
    }

    @Operation(summary = "启动设备")
    @PostMapping("/device/{deviceId}/start")
    @RequirePermission("device:control")
    public Result<Void> startDevice(@PathVariable Long deviceId) {
        dashboardService.startDevice(deviceId);
        return Result.ok();
    }

    @Operation(summary = "停止设备")
    @PostMapping("/device/{deviceId}/stop")
    @RequirePermission("device:control")
    public Result<Void> stopDevice(@PathVariable Long deviceId) {
        dashboardService.stopDevice(deviceId);
        return Result.ok();
    }

    @Operation(summary = "设备控制")
    @PostMapping("/device/{deviceId}/control")
    @RequirePermission("device:control")
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
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "day") String dimension) {
        return Result.ok(dashboardService.getProductionReport(startDate, endDate, dimension));
    }
}
