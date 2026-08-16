# Virtual Path MES 服务器部署指南

> 目标：在 Linux 服务器上部署完整 MES 系统（微服务 + 前端 + AI），并启用**动态设备展示**（3D 数字孪生）。
> 本文档基于当前代码实际行为编写，命令可直接照抄。

---

## 1. 架构总览

```
浏览器 ──https──▶ Cloudflare Tunnel ──http──▶ mes-frontend (nginx:3000, 容器)
                                                  ├── 静态资源 (构建产物内置镜像)
                                                  ├── /api/** ──▶ mes-gateway :9090
                                                  │   ├── /api/auth/**      → mes-auth :8081
                                                  │   ├── /api/workorder/** → mes-workorder :8082
                                                  │   ├── /api/process/**   → mes-process :8083
                                                  │   ├── /api/quality/**   → mes-quality :8084
                                                  │   ├── /api/dashboard/** → mes-dashboard :8085
                                                  │   ├── /api/ws/** (WS)   → mes-dashboard :8085
                                                  │   └── /api/ai/**        → mes-ai-service :8087 (StripPrefix=2)
                                                  └── /health (探活)

中间件: MySQL:3306  Redis:6379  Kafka:9092  Zookeeper:2181  EMQX:1883/18083  InfluxDB:8086
```

**编排文件职责（三文件叠加）**：

| 文件 | 内容 | 何时用 |
|------|------|--------|
| `docker-compose.yml` | 基础设施：mysql / redis / zookeeper / kafka / emqx + mes-network | 必用 |
| `docker-compose.dev.yml` | influxdb + 6 个 Java 微服务（build + 环境变量锚点） | 必用 |
| `docker-compose.prod.yml` | mes-ai-service + mes-frontend（build args） | 必用 |

**动态设备展示数据链路**：

```
设备模拟器 ──HTTP──▶ mes-dashboard(/api/dashboard/device/simulate) ──Redis 实时缓存──▶ WebSocket(/api/ws/dashboard) ──▶ 前端 3D 孪生
```

---

## 2. 服务器要求

| 项目 | 要求 |
|------|------|
| 操作系统 | Linux（Alibaba Cloud Linux / Ubuntu 22.04+ / Debian 12 均可） |
| Docker | 20.10+，含 compose v2 插件 |
| CPU | ≥ 4 核 |
| 内存 | ≥ 8 GB（MySQL 512M + Kafka 512M + 6 个 Java 各 512M + AI 1G + 前端构建峰值） |
| 磁盘 | ≥ 20 GB 可用（构建前端 node_modules 与镜像层需要，建议 30 GB） |
| 出网 | AI 大模型功能需访问智谱 API；npm/pip/apt 已配国内镜像源 |

---

## 3. 快速部署（全新服务器）

### 3.1 获取代码

```bash
git clone https://github.com/xianshi3/virtual-path-mes.git
cd virtual-path-mes
```

> 国内服务器拉不动 GitHub 时，可在本机打包上传（排除 node_modules/.venv/.git，但 **jar 必须单独上传**，见 3.3）。

### 3.2 配置环境变量

```bash
cp .env.example .env
vim .env   # 必填：JWT_SECRET（≥32 位随机串）、ZHIPU_API_KEY（需要大模型功能时）
```

环境变量清单见 [§4](#4-环境变量清单)。

### 3.3 构建 Java 服务 jar【关键】

6 个 Java 服务的 Dockerfile 是 `COPY target/mes-*.jar`（**镜像内不跑 Maven**，服务器无需装 Maven/下载依赖）：

**方案 A（推荐）：本机构建 + 上传 jar**

```powershell
# 本机（需已装 JDK 17 + Maven）
mvn package -DskipTests

# 上传 6 个 jar 到服务器对应目录
scp mes-*/target/mes-*-1.0.0-SNAPSHOT.jar admin@<服务器IP>:/home/admin/virtual-path-mes/
```

**方案 B：服务器直接构建**（服务器需装 JDK 17 + Maven）

```bash
cd /home/admin/virtual-path-mes
mvn package -DskipTests
```

> ⚠️ 常见错误：用 rsync 同步代码时 `--exclude target` 会导致服务器没有 jar、镜像构建失败（`COPY failed: target/... not found`）。

### 3.4 启动全部服务

```bash
cd /home/admin/virtual-path-mes
docker compose -f docker-compose.yml -f docker-compose.dev.yml -f docker-compose.prod.yml up -d --build
```

首次会拉取基础镜像并构建（已配国内源，见 [§5](#5-镜像与构建加速)）。

### 3.5 初始化数据库

**全新部署**：只需导入 `sql/init.sql`（已含全部 30 张表的最新结构，含 device_type 等所有修复）：

```bash
docker exec -i mes-mysql mysql -uroot -p123455 mes_db < sql/init.sql
```

**已有部署升级**：按序号执行新增的 `sql/V*.sql` 迁移（如 V14 补 device_type 列，幂等可重复执行）：

```bash
docker exec -i mes-mysql mysql -uroot -p123455 mes_db < sql/V14__add_device_type.sql
```

### 3.6 验证

```bash
docker compose ps                                # 所有容器应 healthy
curl http://localhost:3000/health                # 前端 nginx → OK
curl http://localhost:9090/actuator/health       # 网关 → {"status":"UP"}
curl http://localhost:8087/api/v1/health         # AI 服务 → {"status":"healthy",...}
```

浏览器访问 `http://<服务器IP>:3000` 或隧道域名，登录后验证：看板 3D 孪生、AI 对话、设备数据。

---

## 4. 环境变量清单

`docker compose` 自动读取根目录 `.env`。所有键见 `.env.example`：

| 变量 | 必填 | 默认值 | 用途 |
|------|:---:|--------|------|
| `JWT_SECRET` | ✅ | - | 网关/Java 服务/AI 共享的 HS256 密钥，≥32 字符 |
| `ZHIPU_API_KEY` | ⚠️ | - | 智谱 AI Key；不填则大模型功能降级（预测/分析不受影响） |
| `MYSQL_ROOT_PASSWORD` | - | `123455` | MySQL root 密码（Java 服务与 AI 同用） |
| `INFLUXDB_ADMIN_PASSWORD` | - | `123455` | InfluxDB 管理密码 |
| `INFLUXDB_TOKEN` | - | 固定值 | InfluxDB token（compose 已内置默认值，重建不变） |
| `EMQX_DEFAULT_PASSWORD` | - | `public` | EMQX 控制台密码 |
| `CORS_ALLOWED_ORIGINS` | - | 已预设 | 网关跨域白名单（逗号分隔，含 `https://*.reality-blog.asia`） |
| `AI_PORT` / `AI_HOST` | - | `8087` / `0.0.0.0` | AI 服务监听覆盖 |

> 容器内互连变量（`MYSQL_HOST`/`REDIS_HOST`/`KAFKA_BOOTSTRAP_SERVERS`/`MES_*_URI` 等）已由 compose 的 `x-common-env` 锚点统一注入，**无需也不要在 `.env` 中重复设置**。

---

## 5. 镜像与构建加速（国内服务器）

| 依赖 | 默认源 | 配置位置 |
|------|--------|----------|
| npm | npmmirror（`registry.npmmirror.com`） | `mes-frontend/Dockerfile` ARG `NPM_REGISTRY` |
| pip | 清华源（`pypi.tuna.tsinghua.edu.cn`） | `mes-ai-service/Dockerfile` ARG `PIP_INDEX` |
| apt | 阿里云 Debian 源 | 6 个 Java Dockerfile 内置 sed 替换 |
| Docker 基础镜像 | Docker Hub | 服务器 `/etc/docker/daemon.json` 配镜像加速器 |

```json
// /etc/docker/daemon.json（改完 systemctl restart docker）
{
  "registry-mirrors": ["https://docker.m.daocloud.io", "https://dockerproxy.net"]
}
```

### 5.1 前端构建参数

构建期变量通过 compose `build.args` 注入（`.env.production` 不进入镜像上下文）：

| 参数 | 值 | 说明 |
|------|-----|------|
| `VITE_API_BASE_URL` | `/api` | axios 全局 baseURL，唯一 `/api` 前缀来源 |
| `VITE_AI_SERVICE_URL` | `/ai` | 相对路径，拼接后 `/api/ai/**` |

> ⚠️ **路径规范**：禁止把 `VITE_AI_SERVICE_URL` 写成 `/api/ai`——会与 baseURL 双重拼接成 `/api/api/ai/**`，导致 AI 接口 404/挂起（历史事故）。

### 5.2 AI 服务镜像

- 依赖已按代码真实 import 精简（**无 torch/CUDA**），镜像约几百 MB
- `requirements.txt` 仅运行时依赖；训练导出 ONNX 另需 `pip install onnxmltools onnx`（可选）
- 镜像自带 HEALTHCHECK（`/api/v1/health`）；容器启动自动建表（`CREATE TABLE IF NOT EXISTS`）
- MySQL/Redis 不可用时自动降级（对话历史缓存不可用，LLM/预测/分析不受影响）

---

## 6. Nginx 与 Cloudflare Tunnel

### 6.1 容器 nginx（唯一入口，无需额外装）

`mes-frontend` 内置 `nginx:alpine`（配置 `mes-frontend/nginx.conf`），已含：
- Vue history 路由 fallback、静态资源 30 天缓存 + gzip
- `/api/` 反代网关（WebSocket 标准升级写法，路径 `/api/ws/dashboard`）
- Cloudflare Flexible SSL 的 `X-Forwarded-Proto` 透传（后端正确识别 https）
- `client_max_body_size 20m`、`server_tokens off`、`/health` 探活

### 6.2 宿主机已装的 nginx

**方案 A（推荐）**：停用 —— Tunnel 直连 3000，宿主机 nginx 不需要：

```bash
sudo systemctl stop nginx && sudo systemctl disable nginx
```

**方案 B**：只做 80 → 3000 转发：

```nginx
# /etc/nginx/conf.d/mes.conf
server {
    listen 80 default_server;
    server_name _;
    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
    }
}
sudo nginx -t && sudo systemctl reload nginx
```

### 6.3 Cloudflare 设置

- `cloudflared` 隧道指向 **`localhost:3000`**（容器 nginx，不是 80）
- **SSL/TLS 模式必须为 Flexible**（CF 代理 HTTPS → 源站 HTTP）
- CORS 默认已放行 `https://*.reality-blog.asia`；换域名更新 `CORS_ALLOWED_ORIGINS`

---

## 7. 动态设备展示（数字孪生数据源）

3D 孪生靠 `mes-dashboard` 的 WebSocket 每 5 秒广播设备状态；设备数据可由：
- **WPF 模拟器**（Windows 本地开发用，`mes-device-simulator-wpf`）
- **HTTP 直调**（headless，服务器推荐）：定时 POST `/api/dashboard/device/simulate`
- **MQTT 全链路**（生产级）：设备 → EMQX → Kafka → dashboard（.NET 网关需自行容器化）

> headless 模拟器脚本 `scripts/device-simulator.py` 尚未实现；需要"设备自动动起来"时再开发。

---

## 8. 日常运维

```bash
# 查看状态/日志
docker compose ps
docker compose logs -f mes-dashboard
docker compose logs -f mes-ai-service

# 更新部署（代码变更后）
git pull
# 有 Java 源码变更 → 重新上传 6 个 jar（见 3.3）
docker compose -f docker-compose.yml -f docker-compose.dev.yml -f docker-compose.prod.yml up -d --build

# 只重建单个服务（如前端改版）
docker compose -f docker-compose.yml -f docker-compose.dev.yml -f docker-compose.prod.yml up -d --build mes-frontend

# 清理（磁盘紧张时）
docker system prune -a -f     # 清无用镜像/构建缓存
docker compose down           # 停止（保留数据卷）
docker compose down -v        # 停止并清数据卷（⚠️ 慎用，丢数据）

# 数据备份
docker exec mes-mysql mysqldump -uroot -p123455 mes_db > backup-$(date +%F).sql
```

---

## 9. 端口一览与安全

| 端口 | 服务 | 建议 |
|------|------|------|
| 3000 | 前端 (nginx) | 对外（经 Tunnel） |
| 9090 | 网关 | 可选直连，建议仅内网 |
| 8081-8085, 8087 | 后端/AI 微服务 | 仅容器内网 |
| 3306/6379/9092/2181/1883/18083/8086 | 中间件 | 仅容器内网，**务必安全组不放行公网** |

> compose 中端口映射到 0.0.0.0 是为了宿主机排查方便；生产请用安全组限制 3306 等端口。

---

## 10. 常见问题排查

| 症状 | 原因 | 处理 |
|------|------|------|
| Java 镜像构建报 `COPY failed: target/... not found` | 服务器没上传 jar | 见 [§3.3](#33-构建-java-服务-jar关键) |
| 前端 build 卡死 | npm/pip/Docker 拉官方源 | 见 [§5](#5-镜像与构建加速国内服务器) |
| 设备批量创建报 `Unknown column 'device_type'` | 旧库缺列 | `sql/V14__add_device_type.sql`（幂等） |
| dashboard 日志 `Connection to localhost:9092` | Kafka advertised listener 或环境变量名错误 | 已修复于 compose：`KAFKA_BOOTSTRAP_SERVERS=kafka:9092` + advertised `kafka:9092` |
| AI 接口 404 / 挂起 | 前端双重 `/api/api/ai/` 前缀 | 已修复：`VITE_AI_SERVICE_URL=/ai`；旧镜像需重建 |
| WS 实时数据不刷新 | Upgrade 头未透传 | 已内置标准 map 写法；确认用仓库版 nginx.conf |
| 上传/导入 413 | body 超限 | 已内置 `client_max_body_size 20m` |
| 登录后重定向异常/提示 http | Flexible SSL 下协议被覆盖 | 已内置 `X-Forwarded-Proto` 透传 |
| 全站 521/522 | Tunnel 指错端口 | `cloudflared` 指向 3000；`curl http://localhost:3000/health` |
| AI 服务重启失败 | 数据库不可达阻断启动 | 已修复：init_db 降级告警不阻断 |

---

## 11. 数据库迁移说明

- `sql/init.sql`：全量最新 schema（30 张表），**全新部署只导这一个**
- `sql/V2__*.sql` ~ `sql/V14__*.sql`：历史增量迁移，已有部署按序补执行
- V14 用 information_schema 判断列存在，**幂等**，已手工加过列的环境可安全执行
