# Virtual Path MES 服务器部署指南

> 目标：在 Linux 服务器上部署完整 MES 系统，并启用**动态设备展示**（数字孪生 3D）。
> 说明：当前项目**可以直接部署**，但有三处需要补充/调整（见 [部署清单](#4-部署清单) 与 [三个关键缺口](#5-三个关键缺口)）。

---

## 1. 架构总览

```
                        ┌─────────────────────────────────────────────┐
                        │                Linux 服务器                 │
                        │                                             │
  浏览器 ──3000──▶ 前端(nginx)                                        │
                        │  │  /api/*  (HTTP + WebSocket)             │
                        │  ▼                                         │
                        │ 网关 mes-gateway :9090                     │
                        │  ├──▶ mes-auth :8081     (认证)            │
                        │  ├──▶ mes-workorder:8082 (工单)            │
                        │  ├──▶ mes-process :8083  (工艺)            │
                        │  ├──▶ mes-quality :8084   (质量)           │
                        │  ├──▶ mes-dashboard:8085  (看板/孪生)       │
                        │  └──▶ mes-ai-service:8087 (AI)             │
                        │                                             │
                        │  MySQL:3306  Redis:6379  Kafka:9092        │
                        │  EMQX:1883   InfluxDB:8086                 │
                        │                                             │
                        │  [headless 设备模拟器 → MQTT → EMQX]       │
                        └─────────────────────────────────────────────┘
```

**动态设备展示数据链路（服务器版）**：

```
headless设备模拟器 ──MQTT──▶ EMQX ──▶ Kafka ──▶ mes-dashboard ──WebSocket──▶ 前端3D孪生
      (Linux 容器/脚本)                                    每5s广播设备状态
```

> 本地开发版用的是 WPF 桌面模拟器（Windows only），服务器上需换成 headless 模拟器，见 [§5.2](#52-动态设备展示数据源)。

---

## 2. 服务器要求

| 项目 | 要求 |
|------|------|
| 操作系统 | Linux（Ubuntu 22.04+/Debian 12/CentOS 8+ 均可） |
| Docker | 20.10+，含 docker compose v2 插件 |
| CPU | ≥ 4 核（AI 模型推理 + 微服务） |
| 内存 | ≥ 8 GB（MySQL 512M + Kafka 512M + 5个Java服务 各512M + AI 1G） |
| 磁盘 | ≥ 30 GB 可用 |
| 出网 | 需要能访问智谱 API（AI 功能），其余可内网 |

---

## 3. 基础设施（docker-compose.yml 已有）

已在根目录 `docker-compose.yml` 定义：

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| mysql | mysql:8.0.33 | 3306 | mes_db |
| redis | redis:7-alpine | 6379 | 缓存 |
| zookeeper | cp-zookeeper:7.5.0 | 2181 | Kafka 依赖 |
| kafka | cp-kafka:7.5.0 | 9092 | 消息总线 |
| emqx | emqx/emqx:5.8.3 | 1883/18083 | MQTT Broker |

启动：

```bash
docker compose up -d
```

---

## 4. 部署清单

| # | 服务 | 定义位置 | 状态 |
|---|------|---------|------|
| 1 | mysql / redis / kafka / emqx | `docker-compose.yml` | ✅ 已有 |
| 2 | influxdb | `docker-compose.dev.yml` | ✅ 已有 |
| 3 | mes-auth / gateway / workorder / process / quality / dashboard | `docker-compose.dev.yml` | ✅ 已有（含 Dockerfile） |
| 4 | mes-frontend | `mes-frontend/Dockerfile` | ⚠️ 未纳入 compose，需补充 |
| 5 | mes-ai-service | `mes-ai-service/Dockerfile` | ⚠️ 未纳入 compose，需补充 |
| 6 | headless 设备模拟器 | 不存在 | ❌ 需新建（见 §5.2） |

---

## 5. 三个关键缺口

### 5.1 AI 服务与前端未纳入 compose

> **已解决**：`docker-compose.prod.yml` 已创建，补充 mes-ai-service 与 mes-frontend 两个服务；`docker-compose.dev.yml` 的 JWT_SECRET 插值语法已修复；`mes-frontend/nginx.conf` 已增加 WebSocket 升级头；前端 `Dockerfile` 已支持 `VITE_API_BASE_URL` / `VITE_AI_SERVICE_URL` 构建 ARG，`.env.production` 已补充 `VITE_AI_SERVICE_URL=/api/ai`。

合并启动方式：

```yaml
  mes-ai-service:
    build:
      context: ./mes-ai-service
      dockerfile: Dockerfile
    container_name: mes-ai-service
    restart: always
    ports:
      - "8087:8087"
    environment:
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_USERNAME: root
      MYSQL_PASSWORD: ${MYSQL_ROOT_PASSWORD:-123455}
      MYSQL_DATABASE: mes_db
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_SECRET: ${JWT_SECRET}
      ZHIPU_API_KEY: ${ZHIPU_API_KEY}
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - mes-network
    # 注意：AI 服务的 kafka bootstrap 在 config.yaml 硬编码为 localhost:9092，
    # 若启用 Kafka 预测链路，需改为 kafka:9092（见 §5.1 注）

  mes-frontend:
    build:
      context: ./mes-frontend
      dockerfile: Dockerfile
    container_name: mes-frontend
    restart: always
    ports:
      - "3000:3000"
    environment:
      VITE_API_BASE_URL: /api
      VITE_AI_SERVICE_URL: /api/ai
    depends_on:
      - mes-gateway
    networks:
      - mes-network
```

**注 1（AI 服务 Kafka）**：`mes-ai-service/config.yaml` 里 `kafka.bootstrap_servers: "localhost:9092"` 与 `redis.host: "localhost"` 为硬编码。数据库/Redis 连接已支持环境变量覆盖（`MYSQL_HOST`/`REDIS_HOST`，见 `conversation_store.py`/`redis_store.py`），但 **Kafka 未做环境变量覆盖**。若 AI 服务需要消费 Kafka 设备数据，需改 `config.yaml` 为 `${KAFKA_BOOTSTRAP_SERVERS}` 或直接改为 `kafka:9092`（compose 服务名）。不需要 Kafka 预测链路时忽略即可。

**注 2（前端构建变量）**：Vite 的 `VITE_*` 变量在**构建时**注入，Dockerfile 里 `RUN npm run build` 读不到 compose 的 `environment`。正确做法：
- 在 `mes-frontend/.env.production` 写入 `VITE_API_BASE_URL=/api`、`VITE_AI_SERVICE_URL=/api/ai`（随镜像打包），或
- 在 compose `build.args` 传入 ARG 并在 Dockerfile 中 export

**注 3（CORS）**：`mes-gateway/application-docker.yml` 的 `CORS_ALLOWED_ORIGINS` 默认仅 `localhost:3000,localhost:5173`。生产通过网关统一代理同源，前端请求不发跨域，一般无需改；若用独立域名直连网关，设置 `CORS_ALLOWED_ORIGINS: https://your.domain`。

### 5.2 动态设备展示数据源【关键】

本地链路依赖 **WPF 桌面模拟器**（`mes-device-simulator-wpf`，仅 Windows）。Linux 服务器需要 headless 模拟器。两个方案：

**方案 A（推荐，零代码改动）**：复用后端已有的 HTTP 模拟接口
- `mes-dashboard` 提供 `POST /api/dashboard/device/simulate`（见 `DashboardController.simulateDevice`），WPF 模拟器也通过它直连兜底。
- 写一个轻量脚本/容器，定时（0.5~5s）向该接口 POST 设备状态 `{deviceCode, status, temperature, speed}`，dashboard 会写入设备表并更新 Redis 实时缓存 → WebSocket 每 5s 广播 → 前端 3D 孪生实时刷新。
- 脚本可放在 `scripts/device-simulator.py`（Python，`httpx` 已由 AI 服务依赖提供）或直接打成独立容器。

**方案 B（完整 MQTT 链路）**：headless 模拟器发布 MQTT 到 EMQX，再由 .NET 网关（`mes-device-gateway`）转发 Kafka。需要把 .NET 网关也容器化（当前无 Dockerfile）。链路与生产环境更贴近，但工作量更大。

> 建议先走**方案 A**：写一个 Python 脚本模拟 10~20 台设备，直接调 dashboard HTTP 接口，最快看到 3D 孪生动起来。

### 5.3 数据库初始化

首次部署需导入 SQL（`sql/init.sql` + 各 `V*.sql` 迁移，或从现有 MySQL 导出）。MySQL 容器启动后：

```bash
# 进容器执行（首次）
docker exec -i mes-mysql mysql -uroot -p123455 < sql/init.sql
```

> 若要保留本地现有数据，可在本机 `mysqldump` 导出 `mes_db`，再导入服务器。

---

## 6. 部署步骤（汇总）

```bash
# 0. 上传项目到服务器（排除 node_modules/target/.venv）
rsync -av --exclude node_modules --exclude target --exclude .venv ./ user@server:/opt/virtual-path-mes/

# 1. 配置环境变量
cp .env.example .env
# 编辑 .env：设置 JWT_SECRET(≥32位随机) / MYSQL_ROOT_PASSWORD / ZHIPU_API_KEY

# 2. 启动基础设施 + 后端 + 前端 + AI（三文件合并，prod 为补充层）
docker compose -f docker-compose.yml -f docker-compose.dev.yml -f docker-compose.prod.yml up -d --build

# 4. 导入数据库
docker exec -i mes-mysql mysql -uroot -p123455 < sql/init.sql

# 5. 启动 headless 设备模拟器（方案 A）
python scripts/device-simulator.py --devices 15 --interval 2

# 6. 验证
curl http://server:3000/health                 # 前端 OK
curl http://server:9090/api/actuator/health    # 网关 OK
浏览器访问 http://server:3000                   # 登录后看仪表盘 3D 孪生
```

---

## 7. 服务端口一览

| 端口 | 服务 | 外部访问 |
|------|------|---------|
| 3000 | 前端 (nginx) | ✅ 浏览器 |
| 9090 | 网关 | 可选直连 |
| 8081-8085, 8087 | 后端/AI 微服务 | 内部，勿暴露 |
| 3306/6379/9092/1883/18083 | 中间件 | 内部，勿暴露 |

> 安全建议：服务器开启防火墙，只放行 3000（及可选的 22/443）；其余端口仅在 Docker 内网（`mes-network`）互通。

---

## 8. 升级/维护

```bash
docker compose pull && docker compose up -d   # 拉新镜像
docker compose logs -f mes-dashboard          # 看日志
docker compose down                           # 停止（保留数据卷）
docker compose down -v                        # 停止并清数据卷（慎用）
```

---

## 9. 待办（需代码改动，确认后实施）

1. ✅ **新增 `docker-compose.prod.yml`**：加入 mes-frontend + mes-ai-service（含 build ARG 与 WS 配置）——**已完成**
2. **新增 `scripts/device-simulator.py`**：headless 设备模拟器（方案 A，调 dashboard HTTP 接口）——**待实施**
3. **（可选）** AI 服务 `config.yaml` 的 Kafka/Redis 改为环境变量支持（当前 AI 未用 Kafka，Redis/MySQL 已支持 env 覆盖，暂无需改）
4. **（可选）** 为 .NET 网关补 Dockerfile（方案 B 才需要）

> 当前项目**可直接部署**；headless 设备模拟器（#2）是启用"动态设备展示"的关键，需要时请告知。