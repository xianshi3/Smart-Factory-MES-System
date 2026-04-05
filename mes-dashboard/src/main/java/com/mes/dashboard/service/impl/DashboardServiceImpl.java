package com.mes.dashboard.service.impl;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.mes.dashboard.dto.OeeQueryDTO;
import com.mes.dashboard.entity.DeviceStatus;
import com.mes.dashboard.entity.OeeData;
import com.mes.dashboard.entity.ProductionStats;
import com.mes.dashboard.mapper.DeviceStatusMapper;
import com.mes.dashboard.mapper.OeeDataMapper;
import com.mes.dashboard.mapper.ProductionStatsMapper;
import com.mes.dashboard.service.DashboardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 看板管理服务实现类
 * @author MES
 * @description 看板业务逻辑实现
 */
@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    private final DeviceStatusMapper deviceStatusMapper;
    private final OeeDataMapper oeeDataMapper;
    private final ProductionStatsMapper productionStatsMapper;
    private final StringRedisTemplate redisTemplate;
    private InfluxDBClient influxDBClient;

    @Autowired
    public void setInfluxDBClient(@Autowired(required = false) InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    public DashboardServiceImpl(DeviceStatusMapper deviceStatusMapper, OeeDataMapper oeeDataMapper, 
                                 ProductionStatsMapper productionStatsMapper, StringRedisTemplate redisTemplate) {
        this.deviceStatusMapper = deviceStatusMapper;
        this.oeeDataMapper = oeeDataMapper;
        this.productionStatsMapper = productionStatsMapper;
        this.redisTemplate = redisTemplate;
    }

    private static final String CACHE_PREFIX = "dashboard:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(5);

    /**
     * 获取生产总览数据
     * @return 总览数据
     */
    @Override
    public Map<String, Object> getOverview() {
        String cacheKey = CACHE_PREFIX + "overview";
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return parseMap(cached);
        }

        Map<String, Object> overview = new HashMap<>();
        LambdaQueryWrapper<DeviceStatus> onlineQuery = new LambdaQueryWrapper<>();
        onlineQuery.eq(DeviceStatus::getStatus, "ONLINE");
        Long onlineCount = deviceStatusMapper.selectCount(onlineQuery);

        LambdaQueryWrapper<DeviceStatus> alarmQuery = new LambdaQueryWrapper<>();
        alarmQuery.eq(DeviceStatus::getStatus, "ALARM");
        Long alarmCount = deviceStatusMapper.selectCount(alarmQuery);

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        LambdaQueryWrapper<ProductionStats> statsQuery = new LambdaQueryWrapper<>();
        statsQuery.eq(ProductionStats::getStatDate, today);
        ProductionStats todayStats = new ProductionStats();
        List<ProductionStats> statsList = null;

        overview.put("onlineDevices", onlineCount);
        overview.put("alarmCount", alarmCount);
        overview.put("todayStats", statsList);
        overview.put("timestamp", System.currentTimeMillis());

        redisTemplate.opsForValue().set(cacheKey, toJson(overview), CACHE_TTL);
        return overview;
    }

    /**
     * 获取所有设备状态
     * @return 设备状态列表
     */
    @Override
    public List<DeviceStatus> getAllDeviceStatus() {
        return deviceStatusMapper.selectList(null);
    }

    /**
     * 获取今日生产统计
     * @return 今日统计列表
     */
    @Override
    public List<ProductionStats> getTodayStats() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        LambdaQueryWrapper<ProductionStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductionStats::getStatDate, today);
        return productionStatsMapper.selectList(wrapper);
    }

    /**
     * 计算OEE
     * @param dto 查询参数
     * @return OEE数据
     */
    @Override
    public OeeData calculateOee(OeeQueryDTO dto) {
        OeeData oeeData = new OeeData();
        oeeData.setDeviceId(dto.getDeviceId());

        if (oeeData.getRunTime() == null || oeeData.getRunTime() == 0) {
            oeeData.setAvailability(0.0);
            oeeData.setPerformance(0.0);
            oeeData.setQuality(0.0);
            oeeData.setOee(0.0);
            return oeeData;
        }

        double availability = (double) oeeData.getRunTime() / (oeeData.getRunTime() + oeeData.getDowntime());
        double performance = (oeeData.getIdealCycleTime() * oeeData.getTotalProducts()) / oeeData.getRunTime();
        double quality = (double) oeeData.getGoodProducts() / oeeData.getTotalProducts();

        availability = Math.min(availability, 1.0);
        performance = Math.min(performance, 1.0);
        quality = Math.min(quality, 1.0);

        double oee = availability * performance * quality;

        oeeData.setAvailability(round2(availability));
        oeeData.setPerformance(round2(performance));
        oeeData.setQuality(round2(quality));
        oeeData.setOee(round2(oee));

        return oeeData;
    }

    /**
     * 获取趋势数据
     * @param days 天数
     * @return 趋势数据
     */
    @Override
    public Map<String, Object> getTrendData(int days) {
        Map<String, Object> trend = new HashMap<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        trend.put("startDate", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        trend.put("endDate", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        return trend;
    }

    /**
     * 保存设备数据
     * @param data 设备数据
     */
    @Override
    public void saveDeviceData(DeviceStatus data) {
        deviceStatusMapper.insert(data);

        String realtimeKey = CACHE_PREFIX + "device:" + data.getDeviceCode();
        redisTemplate.opsForValue().set(realtimeKey, toJson(data), CACHE_TTL);
    }

    private double round2(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private String toJson(Object obj) {
        return obj.toString();
    }

    private Map<String, Object> parseMap(String json) {
        return new HashMap<>();
    }
}
