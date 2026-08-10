package com.mes.dashboard.service.impl;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.mes.dashboard.entity.DeviceStatus;
import com.mes.dashboard.service.TelemetryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备遥测时序服务实现（InfluxDB）
 * 写入：measurement=device_telemetry，tags=device_code，fields=temperature/speed/pressure/power/status
 */
@Slf4j
@Service
public class TelemetryServiceImpl implements TelemetryService {

    private static final String MEASUREMENT = "device_telemetry";
    private static final String BUCKET_KEY = "mes_metrics";

    private final InfluxDBClient influxDBClient;

    public TelemetryServiceImpl(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    @Override
    public boolean isEnabled() {
        return influxDBClient != null;
    }

    @Override
    public void writeTelemetry(DeviceStatus device) {
        if (!isEnabled() || device == null) {
            return;
        }
        writeTelemetry(
                device.getDeviceCode(),
                device.getStatus(),
                device.getTemperature() != null ? device.getTemperature() : 0,
                device.getSpeed() != null ? device.getSpeed() : 0,
                0, 0);
    }

    @Override
    public void writeTelemetry(String deviceCode, String status, double temperature, double speed, double pressure, double power) {
        if (!isEnabled() || deviceCode == null || deviceCode.isBlank()) {
            return;
        }
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            String line = String.format(
                    "%s,device_code=%s status=\"%s\",temperature=%f,speed=%f,pressure=%f,power=%f",
                    MEASUREMENT,
                    escapeTag(deviceCode),
                    status == null ? "UNKNOWN" : status,
                    temperature, speed, pressure, power);
            writeApi.writeRecord(BUCKET_KEY, "mes", WritePrecision.S, line);
        } catch (Exception e) {
            log.debug("Write telemetry to InfluxDB failed: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getDeviceHistory(String deviceCode, int hours, int interval) {
        Map<String, Object> result = new HashMap<>();
        result.put("times", new ArrayList<String>());
        result.put("temperature", new ArrayList<Double>());
        result.put("speed", new ArrayList<Double>());
        result.put("pressure", new ArrayList<Double>());
        result.put("power", new ArrayList<Double>());
        result.put("enabled", isEnabled());

        if (!isEnabled() || deviceCode == null || deviceCode.isBlank()) {
            return result;
        }

        try {
            int safeHours = Math.max(1, Math.min(hours, 168));
            int safeInterval = Math.max(10, Math.min(interval, 3600));
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                    "|> range(start: -%dh) " +
                    "|> filter(fn: (r) => r._measurement == \"%s\" and r.device_code == \"%s\") " +
                    "|> filter(fn: (r) => r._field == \"temperature\" or r._field == \"speed\" or r._field == \"pressure\" or r._field == \"power\") " +
                    "|> aggregateWindow(every: %ds, fn: mean, createEmpty: false) " +
                    "|> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
                    BUCKET_KEY, MEASUREMENT, escapeTag(deviceCode), safeInterval);

            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(flux);

            List<String> times = new ArrayList<>();
            List<Double> temperatures = new ArrayList<>();
            List<Double> speeds = new ArrayList<>();
            List<Double> pressures = new ArrayList<>();
            List<Double> powers = new ArrayList<>();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Instant time = record.getTime();
                    if (time == null) continue;
                    times.add(OffsetDateTime.ofInstant(time, ZoneOffset.ofHours(8)).format(fmt));
                    temperatures.add(num(record.getValueByKey("temperature")));
                    speeds.add(num(record.getValueByKey("speed")));
                    pressures.add(num(record.getValueByKey("pressure")));
                    powers.add(num(record.getValueByKey("power")));
                }
            }

            result.put("times", times);
            result.put("temperature", temperatures);
            result.put("speed", speeds);
            result.put("pressure", pressures);
            result.put("power", powers);
        } catch (Exception e) {
            log.warn("Query device history from InfluxDB failed: {}", e.getMessage());
            result.put("enabled", false);
        }
        return result;
    }

    private double num(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return 0d;
    }

    private String escapeTag(String value) {
        return value.replace(",", "\\,").replace(" ", "\\ ").replace("=", "\\=");
    }
}
