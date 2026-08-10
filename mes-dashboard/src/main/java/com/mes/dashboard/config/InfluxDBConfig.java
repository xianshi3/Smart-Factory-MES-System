package com.mes.dashboard.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * InfluxDB 配置类
 * 时序数据存储（设备遥测历史）。InfluxDB 不可用时优雅降级（返回 null bean），不影响主流程。
 */
@Slf4j
@Configuration
public class InfluxDBConfig {

    @Value("${influxdb.url:}")
    private String url;

    @Value("${influxdb.token:}")
    private String token;

    @Value("${influxdb.org:mes}")
    private String organization;

    @Value("${influxdb.bucket:mes_metrics}")
    private String bucket;

    @Bean
    public InfluxDBClient influxDBClient() {
        if (!StringUtils.hasText(url)) {
            log.warn("InfluxDB 未配置（influxdb.url 为空），设备历史遥测将不可用");
            return null;
        }
        if (!StringUtils.hasText(token)) {
            log.warn("InfluxDB 未配置（influxdb.token 为空），设备历史遥测将不可用");
            return null;
        }
        log.info("InfluxDB initialized: url={}, org={}, bucket={}", url, organization, bucket);
        return InfluxDBClientFactory.create(url, token.toCharArray(), organization, bucket);
    }
}
