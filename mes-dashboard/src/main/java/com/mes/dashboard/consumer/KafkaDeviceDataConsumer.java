package com.mes.dashboard.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.dashboard.entity.AlarmEvent;
import com.mes.dashboard.entity.DeviceStatus;
import com.mes.dashboard.mapper.AlarmMapper;
import com.mes.dashboard.mapper.DeviceStatusMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaDeviceDataConsumer {

    private final DeviceStatusMapper deviceStatusMapper;
    private final AlarmMapper alarmMapper;
    private final com.mes.dashboard.service.TelemetryService telemetryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 启动兜底扫描：设备已在 ALARM 状态但无活跃告警时补建告警。
     * 覆盖"服务重启后 Kafka 已消费的状态变化事件不再重放"的场景。
     */
    @PostConstruct
    public void reconcileAlarmsOnStartup() {
        try {
            List<DeviceStatus> alarmDevices = deviceStatusMapper.selectList(
                    new LambdaQueryWrapper<DeviceStatus>()
                            .eq(DeviceStatus::getStatus, "ALARM")
                            .eq(DeviceStatus::getDeleted, 0));
            int created = 0;
            for (DeviceStatus d : alarmDevices) {
                Long activeCount = alarmMapper.selectCount(new LambdaQueryWrapper<AlarmEvent>()
                        .eq(AlarmEvent::getDeviceCode, d.getDeviceCode())
                        .eq(AlarmEvent::getStatus, "ACTIVE"));
                if (activeCount != null && activeCount > 0) {
                    continue;
                }
                createAlarmForDevice(d);
                created++;
            }
            if (created > 0) {
                log.info("启动兜底扫描: 为 {} 台 ALARM 设备补建告警", created);
            }
        } catch (Exception e) {
            log.warn("启动告警兜底扫描失败: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "mes-device-data", groupId = "mes-dashboard-consumer")
    public void consumeDeviceData(String message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, Map.class);
            
            String deviceId = (String) data.get("deviceId");
            if (deviceId == null) {
                deviceId = (String) data.get("deviceCode");
            }
            if (deviceId == null) {
                log.warn("Message missing deviceId/deviceCode");
                return;
            }

            DeviceStatus device = findOrCreateDevice(deviceId);
            String oldStatus = device.getStatus();

            updateDeviceStatus(device, data);
            
            deviceStatusMapper.updateById(device);

            // 状态变化 → 自动产生/解决告警（报警中心真实数据源）
            handleAlarmTransition(device, oldStatus);

            // 遥测写入 InfluxDB（历史趋势数据源）
            telemetryService.writeTelemetry(device);

            log.debug("Updated device {} status", deviceId);
            
        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}", e.getMessage());
        }
    }

    /**
     * 设备状态变化驱动告警生命周期：
     * - 进入 ALARM：无该设备活跃告警时自动创建（按温度推断级别）
     * - 离开 ALARM：自动解决该设备全部活跃告警（SYSTEM 自动关闭）
     */
    private void handleAlarmTransition(DeviceStatus device, String oldStatus) {
        String newStatus = device.getStatus();
        if (newStatus == null) return;
        boolean wasAlarm = "ALARM".equalsIgnoreCase(oldStatus);
        boolean isAlarm = "ALARM".equalsIgnoreCase(newStatus);

        if (isAlarm && !wasAlarm) {
            Long activeCount = alarmMapper.selectCount(new LambdaQueryWrapper<AlarmEvent>()
                    .eq(AlarmEvent::getDeviceCode, device.getDeviceCode())
                    .eq(AlarmEvent::getStatus, "ACTIVE"));
            if (activeCount != null && activeCount > 0) {
                return; // 已有活跃告警，不重复
            }
            createAlarmForDevice(device);
        } else if (!isAlarm && wasAlarm) {
            List<AlarmEvent> actives = alarmMapper.selectList(new LambdaQueryWrapper<AlarmEvent>()
                    .eq(AlarmEvent::getDeviceCode, device.getDeviceCode())
                    .eq(AlarmEvent::getStatus, "ACTIVE"));
            for (AlarmEvent a : actives) {
                a.setStatus("RESOLVED");
                a.setResolveTime(LocalDateTime.now());
                a.setResolveUser("SYSTEM");
                a.setRemarks("设备状态恢复正常，自动解决");
                alarmMapper.updateById(a);
                log.info("自动解决告警: {} ({} 状态恢复)", a.getAlarmCode(), device.getDeviceCode());
            }
        }
    }

    /** 按温度推断告警级别：>85 严重 / >70 警告 / 其余提示 */
    private String resolveLevel(double temperature) {
        if (temperature > 85) return "CRITICAL";
        if (temperature > 70) return "WARNING";
        return "INFO";
    }

    /** 为设备创建活跃告警（调用方负责去重判断） */
    private void createAlarmForDevice(DeviceStatus device) {
        double temp = device.getTemperature() == null ? 0 : device.getTemperature();
        AlarmEvent alarm = new AlarmEvent();
        alarm.setAlarmCode("ALM-" + device.getDeviceCode() + "-" + System.currentTimeMillis());
        alarm.setLevel(resolveLevel(temp));
        alarm.setAlarmType("DEVICE_STATUS");
        alarm.setDeviceCode(device.getDeviceCode());
        alarm.setDeviceName(device.getDeviceName());
        alarm.setStatus("ACTIVE");
        alarm.setDeleted(0);
        alarm.setOccurrenceTime(LocalDateTime.now());
        alarm.setMessage(buildAlarmMessage(device, temp));
        alarmMapper.insert(alarm);
        log.info("自动创建告警: {} ({}), level={}, temp={}°C",
                alarm.getAlarmCode(), device.getDeviceCode(), alarm.getLevel(), temp);
    }

    private String buildAlarmMessage(DeviceStatus device, double temp) {
        String name = device.getDeviceName() != null ? device.getDeviceName() : device.getDeviceCode();
        if (temp > 85) {
            return String.format("%s 温度过高 (%.1f°C)，存在严重过热风险", name, temp);
        }
        if (temp > 70) {
            return String.format("%s 温度偏高 (%.1f°C)，请关注散热状态", name, temp);
        }
        return String.format("%s 状态异常 (%.1f°C)，已进入告警", name, temp);
    }

    private DeviceStatus findOrCreateDevice(String deviceId) {
        LambdaQueryWrapper<DeviceStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceStatus::getDeviceCode, deviceId).last("LIMIT 1");
        DeviceStatus existing = deviceStatusMapper.selectOne(wrapper);
        
        if (existing != null) {
            return existing;
        }
        
        DeviceStatus newDevice = new DeviceStatus();
        newDevice.setDeviceCode(deviceId);
        newDevice.setDeviceName("设备-" + deviceId);
        newDevice.setDeviceType("Sensor");
        newDevice.setStatus("ONLINE");
        newDevice.setTemperature(30.0);
        newDevice.setSpeed(0.0);
        newDevice.setLastHeartbeat(LocalDateTime.now());

        try {
            deviceStatusMapper.insert(newDevice);
            log.info("Created new device: {}", deviceId);
            return newDevice;
        } catch (DuplicateKeyException e) {
            // 并发首条消息同时到达：唯一约束冲突，重查其他线程已创建的设备，避免丢消息
            DeviceStatus created = deviceStatusMapper.selectOne(
                    new LambdaQueryWrapper<DeviceStatus>()
                            .eq(DeviceStatus::getDeviceCode, deviceId)
                            .last("LIMIT 1"));
            if (created != null) {
                return created;
            }
            throw e;
        }
    }

    private void updateDeviceStatus(DeviceStatus device, Map<String, Object> data) {
        device.setLastHeartbeat(LocalDateTime.now());
        
        // 兼容三种消息结构：
        // 1) .NET 网关转发: { deviceId, timestamp, dataType, status, data: { temperature, speed } }
        // 2) 原始协议:     { deviceId, params: { temperature, speed } }
        // 3) 平铺结构:     { deviceCode, temperature, speed }
        Map<String, Object> params = null;
        Object rawParams = data.get("params");
        if (rawParams instanceof Map) {
            params = (Map<String, Object>) rawParams;
        } else {
            rawParams = data.get("data");
            if (rawParams instanceof Map) {
                params = (Map<String, Object>) rawParams;
            }
        }

        if (params != null) {
            if (params.containsKey("temperature")) {
                Object temp = params.get("temperature");
                if (temp instanceof Number) {
                    device.setTemperature(((Number) temp).doubleValue());
                }
            }
            if (params.containsKey("speed")) {
                Object speed = params.get("speed");
                if (speed instanceof Number) {
                    device.setSpeed(((Number) speed).doubleValue());
                }
            }
        } else {
            // 平铺结构兜底
            Object temp = data.get("temperature");
            if (temp instanceof Number) {
                device.setTemperature(((Number) temp).doubleValue());
            }
            Object speed = data.get("speed");
            if (speed instanceof Number) {
                device.setSpeed(((Number) speed).doubleValue());
            }
        }
        
        Object status = data.get("status");
        if (status != null) {
            device.setStatus((String) status);
        }
        
        Object workstationId = data.get("workstationId");
        if (workstationId instanceof Number) {
            device.setWorkstationId(((Number) workstationId).longValue());
        }
        
        Object productionLineId = data.get("productionLineId");
        if (productionLineId instanceof Number) {
            device.setProductionLineId(((Number) productionLineId).longValue());
        }
    }
}