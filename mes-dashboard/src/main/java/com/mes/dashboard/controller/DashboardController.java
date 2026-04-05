package com.mes.dashboard.controller;

import com.mes.dashboard.dto.OeeQueryDTO;
import com.mes.dashboard.entity.DeviceStatus;
import com.mes.dashboard.entity.OeeData;
import com.mes.dashboard.entity.ProductionStats;
import com.mes.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
