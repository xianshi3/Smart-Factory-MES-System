# Smart Factory MES System 开发文档

---

## 1. 项目概述

智能工厂制造执行系统 (MES)，基于微服务架构，支持2000+设备并发连接。

### 1.1 项目结构

```
Smart-Factory-MES-System/
├── mes-common/              # 公共模块 (Result, BaseEntity, 异常处理)
├── mes-gateway/            # API网关 (9090)
├── mes-auth/                # 认证服务 (8081)
├── mes-workorder/           # 工单服务 (8082)
├── mes-process/             # 工艺服务 (8083)
├── mes-quality/             # 质量服务 (8084)
├── mes-dashboard/           # 看板服务 (8085)
├── mes-device-gateway/      # .NET设备网关 (5000)
├── mes-ai-service/          # Python AI服务 (8087)
├── mes-device-simulator-wpf/ # WPF设备模拟器
├── mes-frontend/            # Vue 3前端 (3000)
├── sql/                      # 数据库脚本
├── Makefile                    # 统一启动命令
├── docker-compose.yml        # 基础设施配置
├── docs/                    # 项目文档
└── README.md                # 项目简介

### 1.2 技术栈

| 层级 | 技术选型 | 版本 |
|------|----------|------|
| 前端 | Vue 3 + TypeScript + Element Plus + ECharts + Three.js | Vue 3.5 |
| 后端 | Java 17 + Spring Cloud + MyBatis-Plus | Spring Cloud 2022.0.0 |
| 设备接入 | .NET 8 + MQTT + Kafka | .NET 8 |
| AI服务 | Python 3.12 + FastAPI + PyMySQL + LightGBM + XGBoost | FastAPI 0.115 |
| 基础设施 | MySQL 8.0.33 + Redis 7 + Kafka 3.4 + EMQX 5.8 | - |

### 1.3 环境配置

敏感信息通过 `.env.local` 文件配置（不提交到git）：

```bash
# mes-ai-service/.env.local
ZHIPU_API_KEY=your-api-key-here
```

---

## 2. 快速开始

### 2.1 前置条件

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | Java运行环境 |
| Maven | 3.9+ | 项目构建 |
| Node.js | 18+ | 前端开发 |
| Python | 3.12+ | AI服务运行环境 |

### 2.2 依赖安装

```bash
# Java (项目根目录)
mvn clean install -DskipTests

# Python AI 服务
cd mes-ai-service
pip install -r requirements.txt
```

> **注意**: Python 3.12 需要 `numpy>=2.0`，`requirements.txt` 已放宽版本约束。首次安装需确保 `pymysql` 和 `sniffio` 正确安装。
| .NET | 8.0+ | 设备网关运行环境 |
| Docker | 24+ | 容器化部署 |

### 2.2 启动所有服务

```bash
make all
```

### 2.3 单独启动服务

```bash
make docker       # 启动基础设施
make backend      # 编译 + 启动 Java 后端
make ai           # 启动 AI 服务
make frontend     # 启动前端
make gateway      # 启动单一 Java 服务
make status       # 查看运行状态
make stop         # 停止所有服务
make clean        # 清理缓存
```

### 2.4 启动前端

```bash
cd mes-frontend
npm install
npm run dev
```

访问 http://localhost:3000

---

## 3. 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 | 3000 | Vue 3应用 |
| API网关 | 9090 | Spring Cloud Gateway（已启用） |
| 认证服务 | 8081 | 用户登录/注册 |
| 工单服务 | 8082 | 工单管理 |
| 工艺服务 | 8083 | 工艺模板 |
| 质量服务 | 8084 | 质检追溯 |
| 看板服务 | 8085 | OEE/WebSocket |
| AI服务 | 8087 | 质量/产量预测 |
| .NET设备网关 | 5000 | MQTT/Kafka数据接入 |
| 设备模拟器 | 8883 | 模拟2000+设备数据上报 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存/黑名单/限流/在线心跳/分布式序号 |
| MQTT | 1883 | 设备通信 |
| Kafka | 9092 | 消息队列 |

> 注意：开发环境前端可通过 Vite 代理直连各服务（8081-8085），或通过网关（9090）统一路由。网关已启用，
> `/api/**` 路由使用 StripPrefix=1（去掉 `/api` 前缀后转发），`/api/ai/**` 路由使用 StripPrefix=2（去掉 `/api/ai` 前缀后转发）。

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
| mes-gateway | 9090 | API路由、统一认证 |

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

### 5.4 Redis 使用规范

**通用原则：Redis 不可用时必须优雅降级，禁止阻断主流程。**

| 场景 | Key 设计 | TTL | 实现位置 | 降级策略 |
|------|----------|-----|----------|----------|
| Token 黑名单 | `auth:blacklist:{sha256(token)}` | 剩余有效期 | mes-auth AuthService/JwtAuthFilter | 跳过校验，放行 |
| 登录失败锁定 | `auth:fail:{username}` | 900s（5次锁定） | mes-auth AuthService | 跳过计数，不锁定 |
| 工单号序号 | `wo:seq`（INCR） | 永久 | mes-workorder | 降级为毫秒时间戳 |
| 设备在线心跳 | `device:online:{deviceId}` | 90s | .NET 网关 DeviceHeartbeatService | 跳过写入 |
| 分析历史缓存 | `analysis:recent:{user}:{device}`（zset） | 300s | mes-ai-service redis_store | 回查 MySQL |
| LLM 结果缓存 | `llm:cache:{hash}` | 3600s | mes-ai-service redis_store | 直连大模型 |
| LLM 限流 | `ratelimit:llm` | 60s 窗口 | mes-ai-service redis_store | 不限流 |
| 看板数据缓存 | `dashboard:*` | 30s | mes-dashboard | 直查数据库 |
| 工艺模板状态 | `template:status:{id}` | 24h | mes-process | 直查数据库 |
| 设备控制指令 | `device:control:{deviceId}` | 5min | mes-dashboard | 指令直发设备 |

**约定：**
- 所有 key 使用 `模块:业务:标识` 三段式命名，`:` 分隔
- 客户端连接统一设置超时（Java/Python 1s，.NET 1s），禁止无限等待
- 缓存数据写入与删除双写同步（如分析历史保存/删除需同步更新缓存）
- Python 侧统一走 `redis_store.py` 封装，禁止散落裸 Redis 调用

### 5.5 前端页面 (mes-frontend)

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
| POST | /auth/login | 用户登录（失败5次锁定15分钟） |
| POST | /auth/logout | 用户登出（Token加入黑名单立即失效） |
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
5. **Docker仅运行基础设施**：MySQL、Redis、Kafka、Zookeeper、Nacos 使用 Docker 运行，Java 服务本地 `java -jar` 启动
6. **AI服务端口**：AI 服务使用 8087 端口，已解决与 InfluxDB 的 8086 端口冲突。
7. **Docker镜像拉取**：如果 Docker 代理无法拉取镜像，需配置镜像加速器（daemon.json 中添加 registry-mirrors）

---

## 10. 常见问题

### Q1: docker 命令找不到
**A**: 安装 Docker Desktop 并重启电脑

### Q2: 前端无法登录
**A**: 检查数据库是否初始化，确认账号 admin/admin123 存在

### Q3: 微服务无法启动
**A**: 检查端口占用，确保基础设施服务正常运行

---

*最后更新：2026-08-02*
