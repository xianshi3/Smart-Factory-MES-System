# 智能工厂MES系统 - 简历项目描述

## 项目概述

**Smart Factory MES System** - 智能工厂制造执行系统

基于微服务架构的工业互联网平台，为离散制造业提供生产制造全流程数字化解决方案。

---

## 核心技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Element Plus + ECharts + Vite + Pinia |
| 后端 | Java 17 + Spring Cloud + MyBatis-Plus + Redis + Kafka |
| 设备接入 | .NET 8 + MQTT + Kafka |
| AI服务 | Python + FastAPI + LightGBM + XGBoost + ONNX |
| 基础设施 | MySQL + Redis + Kafka + EMQX + InfluxDB + Elasticsearch |

---

## 项目架构

```
mes-gateway/        # API网关 (9090)
mes-auth/           # 用户认证 (8081)
mes-workorder/     # 工单管理 (8082)
mes-process/        # 工艺管理 (8083)
mes-quality/       # 质量管理 (8084)
mes-dashboard/     # 数据看板 (8085)
mes-device/         # 设备接入 (8086)
mes-ai-service/     # AI智能服务
mes-front/          # Vue3前端
```

---

## 核心功能

### 1. 生产管理
- 工单创建、执行、跟踪、归档
- 工艺参数配置与版本管理
- 生产进度实时看板

### 2. 质量管理
- 来料检验、过程检验、成品检验
- SPC统计分析
- 质量追溯与原因分析

### 3. 设备监控
- 2000+设备并发接入
- 实时状态监控与告警
- 设备OEE计算

### 4. AI智能预测 (亮点功能)
- 质量预测 (LightGBM/XGBoost/ONNX)
- 设备故障预测
- 工艺参数推荐
- 产能预测
- 能耗优化
- 大模型智能对话

### 5. 数据可视化
- ECharts实时图表
- 生产报表统计
- OEE趋势分析

---

## 个人职责/简历描述

### 项目经历描述 (示例)

> **智能工厂MES系统 - 核心开发工程师**
>
> 负责生产制造执行系统(WEB+后端+AI)的开发与维护。
>
> **主要工作：**
> - 参与微服务架构设计与实现，采用Spring Cloud构建企业级MES平台
> - 开发Vue3前端页面，实现设备监控、报警管理、生产报表等功能
> - 集成Kafka实现2000+设备实时数据采集与监控
> - 开发AI预测服务，质量预测准确率达95%
> - 优化系统性能，实现毫秒级数据响应
> - 实现Redis缓存、Kafka消息队列、EMQX MQTT设备接入
> - 负责MySQL数据库设计与优化
>
> **技术亮点：**
> - 微服务架构，支持高并发
> - 设备实时监控(WebSocket+Kafka)
> - AI质量预测(ONNX模型部署)
> - 大数据量ES存储查询
> - 主题切换(亮色/暗色)

---

## 技术细节

### 设备数据采集架构
```
设备 → MQTT → Kafka → Redis缓存 → 前端实时展示
                    ↓
              InfluxDB时序存储
```

### AI预测服务架构
```
特征工程 → 模型加载(ONNX/Pickle) → 推理预测 → 结果返回
```

### 前端工程
- Vue 3 Composition API
- TypeScript类型安全
- Pinia状态管理
- Element Plus组件库
- ECharts可视化
- 暗黑主题支持

---

## 系统截图/预览

(待添加实际截图)

---

## 数据库表设计

- mes_user - 用户表
- mes_role - 角色表
- mes_permission - 权限表
- mes_menu - 菜单表
- mes_workorder - 工单表
- mes_process - 工艺参数表
- mes_quality - 质量记录表
- mes_device - 设备表
- mes_alarm - 告警表

---

## 项目地址

```bash
# GitHub
https://github.com/anomalyco/Smart-Factory-MES-System
```

---

## 适合岗位

- Java开发工程师
- 前后端开发工程师
- 工业互联网开发
- 智能制造工程师
- MES开发工程师