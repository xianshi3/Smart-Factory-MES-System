package com.mes.dashboard.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.dashboard.entity.AlarmEvent;
import com.mes.dashboard.entity.DeviceStatus;
import com.mes.dashboard.mapper.AlarmMapper;
import com.mes.dashboard.mapper.DeviceStatusMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Kafka 告警事件消费者
 * 消费 .NET 设备网关转发的 mes-alarm-event（设备状态变更消息）
 * ALARM 状态时写入 dash_alarm_event 并同步设备状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaAlarmEventConsumer {

    private final AlarmMapper alarmMapper;
    private final DeviceStatusMapper deviceStatusMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "mes-alarm-event", groupId = "mes-dashboard-consumer")
    public void consumeAlarmEvent(String message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, Map.class);

            String deviceId = (String) data.get("deviceId");
            if (deviceId == null) {
                deviceId = (String) data.get("deviceCode");
            }
            if (deviceId == null) {
                log.warn("Alarm message missing deviceId/deviceCode");
                return;
            }

            String status = (String) data.get("status");
            if (status == null) {
                log.warn("Alarm message missing status for device {}", deviceId);
                return;
            }

            // 同步设备状态
            updateDeviceStatus(deviceId, status);

            // 仅 ALARM 状态产生告警记录
            if (!"ALARM".equalsIgnoreCase(status)) {
                return;
            }

            DeviceStatus device = findDevice(deviceId);
            AlarmEvent alarm = new AlarmEvent();
            alarm.setAlarmCode("ALM-" + deviceId + "-" + System.currentTimeMillis());
            alarm.setMessage(buildAlarmMessage(deviceId, device, data));
            alarm.setLevel(resolveLevel(data));
            alarm.setAlarmType("DEVICE_STATUS");
            alarm.setDeviceCode(deviceId);
            alarm.setDeviceName(device != null ? device.getDeviceName() : null);
            alarm.setStatus("ACTIVE");
            alarm.setDeleted(0);
            alarm.setOccurrenceTime(resolveOccurrenceTime(data));

            alarmMapper.insert(alarm);
            log.info("Alarm recorded for device {}: {}", deviceId, alarm.getMessage());
        } catch (Exception e) {
            log.error("Failed to process alarm event: {}", e.getMessage());
        }
    }

    private void updateDeviceStatus(String deviceId, String status) {
        try {
            DeviceStatus device = findDevice(deviceId);
            if (device == null) {
                log.warn("Device {} not found for alarm update", deviceId);
                return;
            }
            device.setStatus(status);
            device.setLastHeartbeat(LocalDateTime.now());
            deviceStatusMapper.updateById(device);
        } catch (Exception e) {
            log.warn("Failed to sync device status for {}: {}", deviceId, e.getMessage());
        }
    }

    private DeviceStatus findDevice(String deviceId) {
        LambdaQueryWrapper<DeviceStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceStatus::getDeviceCode, deviceId).last("LIMIT 1");
        return deviceStatusMapper.selectOne(wrapper);
    }

    private String buildAlarmMessage(String deviceId, DeviceStatus device, Map<String, Object> data) {
        Object message = data.get("message");
        if (message != null && !String.valueOf(message).isBlank()) {
            return String.valueOf(message);
        }
        String name = device != null ? device.getDeviceName() : deviceId;
        return String.format("设备 %s(%s) 状态变更为 ALARM", name, deviceId);
    }

    private String resolveLevel(Map<String, Object> data) {
        Object level = data.get("level");
        if (level != null) {
            String lv = String.valueOf(level).toUpperCase();
            if ("CRITICAL".equals(lv) || "WARNING".equals(lv) || "INFO".equals(lv)) {
                return lv;
            }
        }
        return "WARNING";
    }

    private LocalDateTime resolveOccurrenceTime(Map<String, Object> data) {
        Object time = data.get("heartbeatTime");
        if (time == null) {
            time = data.get("timestamp");
        }
        if (time instanceof Number n) {
            try {
                return LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(n.longValue()),
                        java.time.ZoneId.systemDefault());
            } catch (Exception ignored) {
            }
        }
        if (time != null) {
            try {
                return OffsetDateTime.parse(String.valueOf(time)).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
            }
            try {
                return LocalDateTime.parse(String.valueOf(time));
            } catch (DateTimeParseException ignored) {
            }
        }
        return LocalDateTime.now();
    }
}
