# Smart Factory MES System 开发文档

---

## 1. 项目概述

智能工厂制造执行系统 (MES)，基于微服务架构，支持2000+设备并发连接。

### 1.1 项目结构

```
Smart-Factory-MES-System/
├── mes-common/              # 公共模块 (Result, BaseEntity, 异常处理)
├── mes-gateway/            # API网关 (8080)
├── mes-auth/                # 认证服务 (8081)
├── mes-workorder/           # 工单服务 (8082)
├── mes-process/             # 工艺服务 (8083)
├── mes-quality/             # 质量服务 (8084)
├── mes-dashboard/           # 看板服务 (8085)
├── mes-device-gateway/      # .NET设备网关
├── mes-ai-service/          # Python AI服务 (8086)
├── mes-device-simulator/    # 设备模拟器
├── mes-frontend/            # Vue 3前端 (3000)
├── sql/                      # 数据库脚本
├── scripts/                  # 工具脚本
├── start-docker.bat         # 快速启动脚本
├── stop-docker.bat          # 快速停止脚本
├── docker-compose.yml        # 基础设施配置
├── DESIGN.md                 # 技术设计文档
├── DEVELOPMENT.md            # 本文档
├── CHANGELOG.md              # 更新日志
└── README.md                # 项目简介
```

### 1.2 技术栈

| 层级 | 技术选型 | 版本 |
|------|----------|------|
| 前端 | Vue 3 + TypeScript + Element Plus + ECharts | Vue 3.5 |
| 后端 | Java 17 + Spring Cloud + MyBatis-Plus | Spring Cloud 2022.0.0 |
| 设备接入 | .NET 8 + MQTT + Kafka | .NET 8 |
| AI服务 | Python 3.11 + FastAPI + LightGBM + XGBoost | FastAPI 0.115 |
| 基础设施 | MySQL 8.0.33 + Redis 7 + Kafka 3.4 + EMQX 5.8 | - |

---

## 2. 快速开始

### 2.1 前置条件

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | Java运行环境 |
| Maven | 3.9+ | 项目构建 |
| Node.js | 18+ | 前端开发 |
| Docker | 24+ | 容器化部署 |

### 2.2 启动基础设施

```powershell
# 方式1：快速启动（推荐）
start-docker.bat

# 停止服务
stop-docker.bat

# 方式2：手动启动
docker compose up -d
```

### 2.3 启动后端服务

**IDEA 启动步骤**：
1. Maven → Reload Project
2. Build → Rebuild Project
3. 按顺序启动各服务（8080→8085）

```powershell
# 或者命令行启动
mvn clean install -DskipTests
start java -jar mes-gateway/target/mes-gateway-1.0.0-SNAPSHOT.jar
start java -jar mes-auth/target/mes-auth-1.0.0-SNAPSHOT.jar
# ... 其他服务
```

### 2.4 启动前端

在 IDE 中直接运行 Java 主类：

| 模块 | 主类 | 端口 |
|------|------|------|
| mes-gateway | `GatewayApplication.java` | 8080 |
| mes-auth | `AuthApplication.java` | 8081 |
| mes-workorder | `WorkOrderApplication.java` | 8082 |
| mes-process | `ProcessApplication.java` | 8083 |
| mes-quality | `QualityApplication.java` | 8084 |
| mes-dashboard | `DashboardApplication.java` | 8085 |

**IDEA 配置步骤**：
1. 打开项目根目录（Smart-Factory-MES-System）
2. Maven → Reload Project
3. Build → Rebuild Project 重新编译
4. 按顺序启动各个服务（建议按端口顺序：8080→8085）
5. 启动后访问 http://localhost:3000

### 2.5 启动前端

```powershell
cd mes-frontend
npm install
npm run dev
```

访问 http://localhost:3000

### 2.6 启动设备模拟器（可选）

```powershell
cd mes-device-simulator
npm install
npm start
```

---

## 3. 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 | 3000 | Vue 3应用 |
| 认证服务 | 8081 | 用户登录/注册（开发环境直连） |
| 工单服务 | 8082 | 工单管理 |
| 工艺服务 | 8083 | 工艺模板 |
| 质量服务 | 8084 | 质检追溯 |
| 看板服务 | 8085 | OEE/WebSocket |
| API网关 | 9090 | Spring Cloud Gateway（可选） |
| AI服务 | 8086 | 质量/产量预测 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |

> 注意：开发环境前端直连各服务（8081-8084），暂不需要网关

---

## 4. 默认账号

| 服务 | 用户名 | 密码 |
|------|--------|------|
| 前端登录 | admin | admin123 |
| Nacos | nacos | nacos |
| EMQX Dashboard | admin | public |

---

## 5. 模块说明

### 5.1 Java后端模块

| 模块 | 端口 | 职责 |
|------|------|------|
| mes-auth | 8081 | 用户认证、JWT令牌 |
| mes-workorder | 8082 | 工单生命周期、报工 |
| mes-process | 8083 | 工艺模板、参数校验 |
| mes-quality | 8084 | 质检记录、追溯 |
| mes-dashboard | 8085 | 实时看板、OEE |
| mes-gateway | 9090 | API路由（暂未使用） |

### 5.2 .NET 设备网关 (mes-device-gateway)

| 组件 | 说明 |
|------|------|
| MqttConsumerService | MQTT消费服务，支持自动重连 |
| KafkaProducerService | Kafka生产者，使用Channel异步处理 |
| DataCleanseService | 数据清洗服务 |
| GatewayConfig | 网关配置类 |

**技术特点：**
- Channel<T> 高吞吐量消息队列
- MQTT 自动重连机制
- Kafka 幂等生产者
- 健康检查支持

### 5.3 公共模块 (mes-common)

| 类 | 说明 |
|-----|------|
| Result<T> | 统一返回 (code, message, data) |
| PageResult<T> | 分页结果 |
| BaseEntity | 基类实体 (id, createTime, updateTime) |
| BizException | 业务异常 |
| ErrorCode | 错误码枚举 |
| JwtUtils | JWT工具类 |
| MesConstants | 常量定义 |

### 5.3 前端页面 (mes-frontend)

| 页面 | 路由 | 功能 |
|------|------|------|
| 登录 | /login | 用户登录 |
| 首页 | /dashboard | 产量趋势、OEE图表 |
| 工单 | /workorder | 工单CRUD、报工 |
| 工艺 | /process | 模板管理、参数校验 |
| 质量 | /quality | 质检记录、正反向追溯 |
| 设备 | /device | 设备状态监控 |

---

## 6. 数据库表

### 6.1 核心表

| 表名 | 说明 |
|------|------|
| sys_user | 用户表 |
| wo_work_order | 工单表 |
| wo_work_report | 报工记录 |
| proc_template | 工艺模板 |
| proc_parameter | 工艺参数 |
| qms_quality_record | 质检记录 |
| qms_traceability | 追溯数据 |
| dash_device_status | 设备状态 |
| dash_production_stats | 生产统计 |
| dash_oee_data | OEE数据 |

---

## 7. API接口

### 7.1 认证服务

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /auth/login | 用户登录 |
| POST | /auth/register | 用户注册 |
| GET | /auth/info | 获取用户信息 |

### 7.2 工单服务

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /workorder | 创建工单 |
| GET | /workorder/{id} | 工单详情 |
| PUT | /workorder/{id} | 更新工单 |
| POST | /workorder/{id}/issue | 下发工单 |
| POST | /workorder/{id}/start | 开始生产 |
| POST | /workorder/report | 提交报工 |
| GET | /workorder/page | 分页查询 |

### 7.3 看板服务

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /overview | 生产总览 |
| GET | /devices | 设备状态 |
| GET | /production/today | 今日统计 |
| GET | /oee/calculate | OEE计算 |

---

## 8. 消息队列

### 8.1 Kafka主题

| 主题 | 说明 |
|------|------|
| mes-device-data | 设备数据 |
| mes-workorder-event | 工单事件 |
| mes-quality-event | 质量事件 |
| mes-alarm-event | 告警事件 |

### 8.2 MQTT主题

| 主题 | 说明 |
|------|------|
| mes/device/+/data | 设备数据 |
| mes/device/+/status | 设备状态 |
| mes/device/+/control | 控制指令 |

---

## 9. 注意事项

1. **首次启动**：必须执行 sql/init.sql 初始化数据库
2. **Docker命令**：Windows使用 `docker compose`（空格）
3. **前端账号**：admin / admin123
4. **AI模型**：示例模型，需真实数据训练后替换

---

## 10. 常见问题

### Q1: docker 命令找不到
**A**: 安装 Docker Desktop 并重启电脑

### Q2: 前端无法登录
**A**: 检查数据库是否初始化，确认账号 admin/admin123 存在

### Q3: 微服务无法启动
**A**: 检查端口占用，确保基础设施服务正常运行

---

## 11. 版本记录

### v1.0.9 (2026-04-05)
- .NET设备网关优化：Channel异步处理、自动重连、健康检查

### v1.0.8 (2026-04-05)
- 添加CORS跨域配置到workorder、process、quality服务
- 修复前端API响应处理，处理非标准响应格式
- 前端直连各服务（8081-8084），网关端口改为9090

### v1.0.7 (2026-04-05)
- 修复POM配置，添加spring-boot-maven-plugin
- 修复Quality服务路径（/quality/record/page）
- 统一API管理到services.ts

### v1.0.6 (2026-04-05)
- 创建services.ts统一管理各服务API地址
- 修复token获取方式（localStorage）
- 修复Vue文件import路径

### v1.0.2 (2026-04-04)
- 优化 Docker 配置，默认只启动 MySQL + Redis（开发友好）
- 添加简化版 docker-compose.yml
- 添加常见 Docker Hub 拉取失败解决方案

### v1.0.1 (2026-04-04)
- 添加代码注释 (116个文件)
- 创建设备模拟器
- 添加ONNX示例模型
- 修复前端配置警告
- 添加README和默认账号

### v1.0.0 (2026-04-04)
- 项目初始化
- 7个Java微服务
- .NET设备网关
- Python AI服务
- Vue 3前端

---

*最后更新：2026-04-05*