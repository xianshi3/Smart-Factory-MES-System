package com.mes.dashboard.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mes.dashboard.entity.DeviceStatus;
import com.mes.dashboard.mapper.DeviceStatusMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaDeviceDataConsumer {

    private final DeviceStatusMapper deviceStatusMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "mes-device-data", groupId = "mes-dashboard-consumer")
    public void consumeDeviceData(String message) {
        try {
            log.debug("Received Kafka message: {}", message);
            
            Map<String, Object> data = objectMapper.readValue(message, Map.class);
            
            String deviceId = (String) data.get("deviceId");
            if (deviceId == null) {
                log.warn("Message missing deviceId");
                return;
            }

            DeviceStatus device = findOrCreateDevice(deviceId);
            
            updateDeviceStatus(device, data);
            
            deviceStatusMapper.updateById(device);
            log.debug("Updated device {} status", deviceId);
            
        } catch (Exception e) {
            log.error("Failed to process Kafka message: {}", e.getMessage());
        }
    }

    private DeviceStatus findOrCreateDevice(String deviceId) {
        LambdaQueryWrapper<DeviceStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DeviceStatus::getDeviceCode, deviceId);
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
        
        deviceStatusMapper.insert(newDevice);
        log.info("Created new device: {}", deviceId);
        
        return newDevice;
    }

    private void updateDeviceStatus(DeviceStatus device, Map<String, Object> data) {
        device.setLastHeartbeat(LocalDateTime.now());
        
        Map<String, Object> params = (Map<String, Object>) data.get("params");
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