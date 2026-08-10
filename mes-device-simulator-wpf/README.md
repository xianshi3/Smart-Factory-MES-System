# MES设备模拟器 (WPF)

## 简介

WPF桌面设备模拟器，用于模拟MES系统设备数据并推送到后端。

## 功能

- 设备管理：创建、更新、删除设备
- 数据模拟：温度、速度、压力、功率参数控制
- 自动波动：支持温度、速度自动波动，状态随机切换
- **双通道推送**：HTTP（`POST /api/dashboard/device/simulate`）+ MQTT（`mes/device/{deviceCode}/data`）
- 主题切换：亮色/暗色主题

## 运行

```bash
cd mes-device-simulator-wpf
dotnet run
```

## 使用

1. 填写 **API地址**（默认 `http://localhost:8085`）与 **MQTT服务器**（默认 `localhost:1883`，即 docker-compose 的 EMQX）
2. 点击"连接"——自动连接后端 API 与 MQTT Broker
3. 在设备列表中选择或创建新设备
4. 勾选"自动温度/自动转速"调整波动
5. 点击"开始模拟"启动数据推送（每 2 秒一条）

状态栏显示"已连接 API + MQTT"表示双链路就绪；若 MQTT 连接失败会自动降级为仅 HTTP 推送。

## 数据链路

```
模拟器 ──MQTT──▶ EMQX ──▶ .NET设备网关 ──Kafka──▶ 看板服务 ──WebSocket──▶ 前端
   └──────HTTP───────────────────────▶ 看板服务（直连兜底）
```

## 技术

- .NET 8 + WPF
- MQTTnet 4.3.6（MQTT 发布，QoS 1）
- HttpClient 调用后端 REST API
- DispatcherTimer 定时推送数据
