# mes-device-gateway — 设备数据网关（.NET）

MQTT 消息接入 → Kafka 转发的设备数据通道。

## 职责

- 订阅 EMQX MQTT Broker 的设备遥测/告警主题
- 解析设备报文，转发到 Kafka（`mes-device-data` 等主题）供 mes-dashboard 消费
- 配置通过 `appsettings.json`（环境变量 `Gateway__*` 可覆盖）

## 技术栈

.NET 8 · MQTTnet · Confluent.Kafka

## 端口

`5000`（健康检查等辅助端点）

## 快速开始

```bash
cd mes-device-gateway/src/MesDeviceGateway
dotnet run          # 或 make dotnet-gateway
```

前置：EMQX（1883）与 Kafka（9092）已启动（`make docker`）。

## 配置（appsettings.json）

```jsonc
{
  "Gateway": {
    "MqttHost": "localhost",
    "MqttPort": 1883,
    "MqttUsername": "admin",
    "MqttPassword": "public",     // 环境变量 Gateway__MqttPassword 覆盖
    "KafkaBootstrapServers": "localhost:9092"
  }
}
```

## 目录结构

```
src/MesDeviceGateway/
├── Program.cs       # 服务装配与启动
├── appsettings.json # 配置
├── Mqtt/            # MQTT 订阅与报文解析
└── Kafka/           # Kafka 生产者
```

## 相关文档

- [设备模拟器](../mes-device-simulator-wpf/README.md)
- [开发指南](../docs/DEVELOPMENT.md)

*最后更新: 2026-08-15*
