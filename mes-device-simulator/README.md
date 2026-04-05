# MES 设备模拟器

## 简介

用于模拟2000+设备上报数据，测试系统负载和性能。

## 功能特性

- 支持模拟2000+设备（可配置）
- MQTT协议上报设备数据
- 模拟设备参数：温度、压力、速度、振动、功率、运行时长
- 随机生成设备状态变化（ONLINE/OFFLINE/ALARM/MAINTENANCE）
- 批量上报优化，支持高并发测试

## 快速开始

### 安装依赖

```bash
cd mes-device-simulator
npm install
```

### 配置

编辑 `.env` 文件：

```env
# 设备数量
DEVICE_COUNT=2000

# MQTT地址
MQTT_BROKER=mqtt://localhost:1883

# 数据上报间隔(ms)
DATA_INTERVAL=5000

# 状态上报间隔(ms)
REPORT_INTERVAL=10000
```

### 启动

```bash
npm start
```

### Docker运行

```bash
docker build -t mes-device-simulator .
docker run -d --name mes-simulator --env MQTT_BROKER=mqtt://host.docker.internal:1883 mes-device-simulator
```

## 数据格式

### 设备数据上报

Topic: `mes/device/{deviceId}/data`

```json
{
  "deviceId": "DEV0001",
  "timestamp": 1712234567890,
  "status": "ONLINE",
  "params": {
    "temperature": 45.5,
    "pressure": 1.2,
    "speed": 500,
    "vibration": 2.3,
    "power": 75.5,
    "runtime": 3600
  },
  "workstationId": 1,
  "productionLineId": 1
}
```

### 设备状态上报

Topic: `mes/device/{deviceId}/status`

```json
{
  "deviceId": "DEV0001",
  "deviceName": "设备-DEV0001",
  "deviceType": "CNC",
  "status": "ONLINE",
  "temperature": 45.5,
  "speed": 500,
  "lastHeartbeat": "2026-04-04T12:00:00.000Z",
  "workstationId": 1,
  "productionLineId": 1
}
```

## 性能测试

默认配置下：
- 设备数量：2000
- 每5秒上报100条设备数据
- 每10秒上报50条设备状态

可根据需要调整 `DEVICE_COUNT` 和上报间隔进行压力测试。

---

*创建时间: 2026-04-04*