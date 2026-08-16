# Virtual Path MES 开发文档

---

## 1. 项目概述

虚拟路径MES系统 (MES)，基于微服务架构，支持2000+设备并发连接。

### 1.1 项目结构

```
virtual-path-mes-System/
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
| 后端 | Java 17 + Spring Cloud + MyBatis-Plus | Spring Cloud 2023.0.0 |
| 设备接入 | .NET 8 + MQTT + Kafka | .NET 8 |
| AI服务 | Python 3.12 + FastAPI + PyMySQL + LightGBM + XGBoost | FastAPI 0.115 |
| 基础设施 | MySQL 8.0.33 + Redis 7 + Kafka 7.5.0 + EMQX 5.8 | - |

### 1.3 环境配置

敏感信息通过环境变量配置（不提交到git）。**所有 Java 服务启动强制要求 `JWT_SECRET`**（长度 ≥ 32 字符，且所有服务必须一致），未设置则启动失败。

**本地开发（推荐）**：Java 服务已内置 `spring.config.import` 自动读取**项目根目录 `.env`**（`mes-ai-service/.env.local` 单独管理 AI 密钥），IDE 直接启动即可：

```bash
# 复制模板为本地配置（.env 已被 .gitignore 忽略，不会入库）
cp .env.example .env

# 编辑 .env，把 JWT_SECRET 改为随机值（长度 >= 32），所有服务自动生效
# 例: JWT_SECRET=$(openssl rand -base64 48 | tr '+/' '-_' | tr -d '=')
```

**生产/容器**：Docker Compose 或系统环境变量注入（优先级高于 .env）。

| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| `JWT_SECRET` | JWT 签名密钥（≥32 字符），**必填**，所有服务一致 | 无（缺失则启动失败） |
| `MYSQL_PASSWORD` | 数据库密码 | `123455` |
| `MYSQL_ROOT_PASSWORD` | docker-compose 初始化 MySQL root 密码 | `123455` |
| `INFLUXDB_TOKEN` | InfluxDB 访问 token（dashboard 时序存储） | 空（未配置则历史遥测不可用） |
| `INFLUXDB_ADMIN_PASSWORD` | docker-compose.dev.yml InfluxDB 管理员密码 | `123455` |
| `ZHIPU_API_KEY` | AI 服务智谱 API Key（`mes-ai-service/.env.local`） | 空 |
| `CORS_ALLOWED_ORIGINS` | 网关跨域白名单（逗号分隔） | `http://localhost:3000,http://localhost:5173` |

> **安全提醒**：所有凭据均为开发默认值，生产部署必须通过环境变量覆盖（详见 `docs/CHANGELOG.md` v1.0.48）。
> **旧容器升级**：若本地 MySQL 容器是旧密码（如 `root/root`），执行 `ALTER USER 'root'@'%' IDENTIFIED BY '123455';` 或重建容器，保持与 `.env` 一致。

---

## 2. 快速开始

### 2.1 前置条件

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | Java运行环境 |
| Maven | 3.9+ | 项目构建 |
| Node.js | 18+ | 前端开发 |
| Python | 3.12+ | AI服务运行环境 |
| .NET | 8.0+ | 设备网关运行环境 |
| Docker | 24+ | 基础设施容器（MySQL / Redis / Kafka / EMQX） |

### 2.2 依赖安装

```bash
# Java (项目根目录)
mvn clean install -DskipTests

# Python AI 服务
cd mes-ai-service
pip install -r requirements.txt
```

> **注意**: Python 3.12 需要 `numpy>=2.0`，`requirements.txt` 已放宽版本约束。首次安装需确保 `pymysql` 和 `sniffio` 正确安装。
> **AI 服务鉴权**：AI 服务（FastAPI）除 `/api/v1/health` 外全部接口要求有效 JWT（`Authorization: Bearer <token>`），密钥与 Java 后端一致（`JWT_SECRET` 环境变量），Agent 工具调用后端 API 时会自动透传用户 token。

### 2.3 启动所有服务

```bash
make all
```

### 2.4 单独启动服务

```bash
make docker       # 启动基础设施
make backend      # 编译 + 启动 Java 后端
make ai           # 启动 AI 服务
make frontend     # 启动前端
make gateway      # 启动单一 Java 服务
make status       # 查看运行状态
make stop         # 停止所有服务（按端口终止进程）
make clean        # 清理缓存
```

### 2.5 启动前端

```bash
cd mes-frontend
npm install
npm run dev
```

访问 http://localhost:3000

**前端 API 路径约定**：开发环境 `VITE_API_BASE_URL` 为空，浏览器请求由 Vite 代理转发（`/api` → 网关 9090、`/auth` → 8081、`/ai` → AI 服务 8087 等）；生产构建 `VITE_API_BASE_URL=/api`，请求统一走网关（Nginx 需将 `/api` 反向代理到 mes-gateway:9090，并代理 WebSocket `/api/ws/**`）。

---

## 3. 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 | 3000 | Vue 3应用 |
| API网关 | 9090 | Spring Cloud Gateway（JWT 全局鉴权 + CORS 白名单） |
| 认证服务 | 8081 | 用户登录/注册 |
| 工单服务 | 8082 | 工单管理 |
| 工艺服务 | 8083 | 工艺模板 |
| 质量服务 | 8084 | 质检追溯 |
| 看板服务 | 8085 | OEE/WebSocket |
| AI服务 | 8087 | 质量/产量预测、Agent 助理 |
| .NET设备网关 | 5000 | MQTT/Kafka数据接入 |
| 设备模拟器 | - | WPF 桌面客户端（HTTP 客户端 + MQTT 客户端，无监听端口） |
| MySQL | 3306 | 数据库（root / 123455） |
| Redis | 6379 | 缓存/黑名单/限流/在线心跳/分布式序号 |
| InfluxDB | 8086 | 时序数据库（docker-compose.dev.yml） |
| MQTT | 1883 | 设备通信 |
| Kafka | 9092 | 消息队列 |

> **认证链路**：网关 `JwtAuthGlobalFilter` 对所有请求校验 `Authorization: Bearer <JWT>`（白名单：`/api/auth/login`、`/api/auth/register`、`/actuator/**`），校验通过后透传 `X-User-Id` / `X-User-Name` / `X-User-Role` 头；各微服务再经 `TokenAuthFilter` 二次解析并写入 `UserContext`，业务接口按 `@RequireRole` / `@RequirePermission` 做细粒度鉴权（ADMIN 全量放行）。
>
> **WebSocket**：前端通过网关连接 `ws://localhost:9090/api/ws/dashboard?token=<JWT>`（开发环境同源），网关 `/api/ws/**` 路由转发到看板服务，握手时校验 token，未授权连接直接拒绝。生产环境如用 Nginx，需为 `/api/ws/**` 配置 `proxy_set_header Upgrade/Connection`。
>
> **开发直连**：前端可通过 Vite 代理直连各服务（8081-8085），或通过网关（9090）统一路由。网关 `/api/**` 路由使用 StripPrefix=1（去掉 `/api` 前缀后转发），`/api/ai/**` 使用 StripPrefix=2（去掉 `/api/ai` 前缀后转发）。

---

## 4. 默认账号

| 服务 | 用户名 | 密码 |
|------|--------|------|
| 前端登录 | admin | admin123 |
| MySQL (root) | root | 123455 |
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
| BaseEntity | 基类实体 (id, createTime, updateTime, deleted) |
| BizException | 业务异常 |
| ErrorCode | 错误码枚举 |
| JwtUtils | JWT 工具类（HS256；启动校验 `JWT_SECRET` ≥ 32 字符，缺失拒绝启动） |
| TokenAuthFilter | 全局 Token 认证过滤器（白名单 `/auth/login,/auth/register`） |
| SecurityInterceptor | 权限校验拦截器（读取 `@RequireRole` / `@RequirePermission`，ADMIN 全量放行） |
| PermissionService | 权限码解析（role_id → sys_role_permission → sys_permission，5 分钟本地缓存） |
| UserContext | 请求线程用户上下文（userId / username / role） |
| GlobalExceptionHandler | 全局异常处理（业务异常 400、未认证 401、系统错误 500） |
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

> 除登录/注册外，所有接口需携带 `Authorization: Bearer <token>`（网关统一校验，未带/失效返回 401）。
> 写操作按权限码/角色控制（如 `process:create`、`device:control`、`quality:delete`、ADMIN/MANAGER 角色等），权限码以 `sys_permission` 表为准。
> 文档页面：各服务 `http://localhost:{port}/doc.html`（Knife4j，仅开发/测试环境启用）。

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
| GET | /device/{code}/history | 设备历史时序数据（InfluxDB） |
| POST | /device/batch | 批量创建设备 |
| POST | /device/simulate | 模拟数据上报（模拟器单台） |
| DELETE | /devices/all | 清空全部设备 |

### 7.4 AI 生产助理（mes-ai-service /agent）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/agent/run | Agent 执行（四阶段编排：任务理解→计划执行→知识增强→结果交付） |
| GET | /api/v1/agent/tools | 可用工具列表（15 个 MES 工具） |
| POST | /api/v1/agent/kb/search | 知识库检索（TF-IDF） |
| POST | /api/v1/agent/conversations | 新建对话 / 列表 / 详情 / 消息 / 删除 |
| POST | /api/v1/agent/analysis | 分析历史 CRUD |

> **Agent 响应结构**: `{success, content, steps(执行步骤), plan(执行计划), report(结构化交付: summary/key_points/tables/recommendations/follow_ups), intent, intent_label}`。
> **多轮交互**: 传入 `session_id` 后，Agent 通过 Redis 记忆上轮设备焦点，指代（如"那台设备"）自动继承。

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
| mes/device/+/data | 设备数据（遥测） |
| mes/device/+/status | 设备状态变更（网关转发至 Kafka `mes-alarm-event`） |
| mes/device/+/control | 控制指令 |

> **模拟器双通道**: WPF 模拟器同时走 HTTP（`POST /api/dashboard/device/simulate`，批量走 `/api/dashboard/device/batch`）与 MQTT（`mes/device/{deviceCode}/data`）。MQTT payload 采用网关协议结构 `{timestamp, dataType, status, data:{temperature, speed, ...}}`。

---

## 9. 注意事项

1. **首次启动**：必须执行 sql/init.sql 初始化数据库；旧库升级按 V2~V13 顺序执行（脚本已幂等，可重复执行）
2. **Docker命令**：Windows使用 `docker compose`（空格）
3. **前端账号**：admin / admin123
4. **AI模型**：示例模型，需真实数据训练后替换
5. **Docker仅运行基础设施**：MySQL、Redis、Kafka、Zookeeper、EMQX 使用 Docker 运行，Java 服务本地 `java -jar` 启动（`docker-compose.dev.yml` 提供全容器方案 + InfluxDB）
6. **AI服务端口**：AI 服务使用 8087 端口，已解决与 InfluxDB 的 8086 端口冲突
7. **Docker镜像拉取**：`docker-daemon.json` 已配置可用镜像加速源（daocloud/1panel/rat）；若直连 Docker Hub 超时可自建代理，注意 USTC/163/百度/docker-cn 等源均已停服
8. **设备模拟器**：启动时自动探测 API 网关与 EMQX 地址（失败回退 localhost）；连接 API 后自动加载已有设备，勾选"参与模拟"的复选框可批量选控；数据推送频率、场景、参数自动持久化到 `%APPDATA%/MESDeviceSimulator/config.json`
9. **JWT 密钥**：所有 Java 服务与 AI 服务必须配置一致的 `JWT_SECRET`（≥ 32 字符），缺失则启动失败；严禁使用默认/公开密钥部署生产
10. **密码修改**：管理员在「用户管理」创建/更新用户时，密码自动 BCrypt 加密落库；登录不再支持明文兜底
11. **停服命令**：`make stop` 按端口（8081-8085/9090/8087/5000/3000）终止进程，不再依赖窗口标题

---

## 10. 常见问题

### Q1: docker 命令找不到
**A**: 安装 Docker Desktop 并重启电脑

### Q2: 前端无法登录
**A**: 检查数据库是否初始化，确认账号 admin/admin123 存在；再确认各服务 `JWT_SECRET` 环境变量一致（不一致会导致 token 解析失败）

### Q3: 微服务无法启动
**A**: 检查端口占用，确保基础设施服务正常运行；确认已设置 `JWT_SECRET`（缺失或长度 < 32 时服务启动即失败，日志会提示）

### Q4: 生产环境接口 404
**A**: 前端生产构建 `VITE_API_BASE_URL=/api`，请求必须经网关转发（Nginx 将 `/api/**` 代理到 mes-gateway:9090）；若直接部署到子路径，需同步调整 Nginx 与 `CORS_ALLOWED_ORIGINS`

### Q5: WebSocket 连不上
**A**: 前端 WebSocket 走网关 `/api/ws/dashboard?token=<JWT>`；检查网关是否已启动、token 是否有效（过期会 401 拒绝握手）；Nginx 场景需配置 WebSocket Upgrade 头

---

*最后更新：2026-08-14*