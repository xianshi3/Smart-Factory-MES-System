# mes-dashboard — 看板服务

设备监控、告警管理、BOM/物料/库存、WebSocket 实时推送、OEE/趋势统计。

## 功能

### 设备监控（`/dashboard`）

- 设备 CRUD / 批量创建 / 启停控制（Redis 控制指令 5 分钟 TTL）
- 模拟数据写入、全量设备查询、实时状态（Kafka `mes-device-data` 消费自动建档，唯一键冲突重查）
- OEE 计算（可用率/性能率/质量率，空值防护）、趋势统计（days 1~366 上限）

### 告警管理（`/alarm`）

- 告警 CRUD、确认（ACK）/解决（RESOLVE）、按状态查询
- **真实数据链路**（v1.0.51）：设备状态进入 ALARM 自动创建告警（按温度分级
  >85 严重 / >70 警告 / 其余提示），恢复正常自动解决（SYSTEM）；服务启动时
  兜底扫描补齐存量告警设备，不依赖手工种子数据

### BOM/物料/库存（`/dashboard/bom` `/material` `/inventory`）

- BOM 与行项 CRUD、BOM 校验（物料存在性 + 循环引用检测）
- 库存调整：条件更新防并发（`quantity >= |delta|`），交易流水留痕
- 库存 `available_quantity` 为数据库生成列（quantity - locked_quantity），只读

### 实时推送

- WebSocket `/ws/dashboard`：5 秒广播设备状态（握手校验 `?token=` JWT）
- 经网关 `/api/ws/**` 路由接入

### 时序存储

- 设备遥测写入 InfluxDB（`INFLUXDB_TOKEN` 未配置时优雅降级），行协议字段转义

## 技术栈

Spring Boot 3.2.5 · MyBatis-Plus · WebSocket · Kafka · InfluxDB 2.x · Knife4j

## 端口

`8085`

## 核心接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/dashboard/devices` | 全量设备状态 | Bearer |
| POST | `/dashboard/device` | 创建设备 | device:control |
| PUT | `/dashboard/device` | 更新设备 | device:control |
| DELETE | `/dashboard/device/{code}` | 删除设备 | device:control |
| POST | `/dashboard/device/{id}/start` | 启动设备 | device:control |
| POST | `/dashboard/device/simulate` | 模拟数据 | device:control |
| GET | `/dashboard/overview` | 总览统计 | Bearer |
| GET | `/dashboard/trend?days=7` | 趋势数据 | Bearer |
| GET/POST/DELETE | `/alarm*` | 告警查询/创建/确认/解决 | 查询 Bearer，写 ADMIN/MANAGER |
| GET/POST/PUT/DELETE | `/dashboard/bom*` | BOM 管理 | 查询 Bearer，写 ADMIN/MANAGER |
| GET/POST/PUT/DELETE | `/dashboard/material*` | 物料管理 | 同上 |
| POST | `/dashboard/inventory/adjust` | 库存调整 | ADMIN/MANAGER |
| WS | `/ws/dashboard?token=` | 实时广播 | JWT |

## 相关数据表

`dash_device_status` · `dash_production_stats` · `dash_oee_data` · `alarm_event` · `inventory` · `inventory_transaction`

## 相关文档

- [数据库设计](../docs/DATABASE.md)
- [AI 助手（设备健康/报表解读工具）](../mes-ai-service/README.md)

*最后更新: 2026-08-15*
