# Smart Factory MES System

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Cloud-2022.0.0-brightgreen?style=for-the-badge&logo=spring" alt="Spring Cloud">
  <img src="https://img.shields.io/badge/Vue-3.5-brightgreen?style=for-the-badge&logo=vue.js" alt="Vue 3">
  <img src="https://img.shields.io/badge/Java-17-brightgreen?style=for-the-badge&logo=java" alt="Java 17">
  <img src="https://img.shields.io/badge/.NET-8-brightgreen?style=for-the-badge&logo=.NET" alt=".NET 8">
  <img src="https://img.shields.io/badge/Python-3.11-brightgreen?style=for-the-badge&logo=python" alt="Python 3.11">
</p>

<p align="center">
  <a href="https://github.com/anomalyco/opencode/issues">Issues</a> •
  <a href="#项目展示">Screenshots</a> •
  <a href="#快速开始">Getting Started</a> •
  <a href="docs/DESIGN.md">Architecture</a> •
  <a href="docs/DEVELOPMENT.md">Development</a>
</p>

---

## 项目简介

智能工厂制造执行系统（Manufacturing Execution System，MES）是面向离散制造业的工业互联网平台，基于微服务架构设计，支持2000+设备并发连接，实现生产过程的实时监控、质量追溯和智能预测。

### 核心特性

- **微服务架构**：基于 Spring Cloud 2022 设计 6 大核心服务，实现服务解耦和独立部署
- **高并发设备接入**：MQTT 协议采集，Kafka 消息队列实现 10000+ 条/秒数据吞吐
- **实时数据展示**：WebSocket 毫秒级推送，InfluxDB 时序数据库存储
- **AI 智能预测**：Python FastAPI 推理服务，集成 LightGBM/XGBoost 模型，支持 ONNX 部署
- **全链路权限控制**：JWT + Spring Security 实现 RBAC 菜单级/按钮级权限

---

## 项目展示

![生产看板](https://github.com/user-attachments/assets/fcf3781d-48a0-4ee8-ac48-3abf60fdca40)

![设备监控](https://github.com/user-attachments/assets/0483ce1a-dcf8-4e3f-aca5-46ef77ada032)

![AI预测](https://github.com/user-attachments/assets/db4d21b4-2a3f-4878-8d19-940ebb0bc94e)

---

## 技术架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         前端展示层                                     │
│              Vue 3 + TypeScript + Element Plus + ECharts               │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         API 网关层                                     │
│                    Spring Cloud Gateway (9090)                         │
└─────────────────────────────────────────────────────────────────────┘
                                    │
          ┌───────────────┬───────────────┬───────────────┐
          ▼               ▼               ▼               ▼
   ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
   │ 认证服务  │    │ 工单服务  │    │ 工艺服务  │    │ 质量服务  │
   │  8081   │    │  8082   │    │  8083   │    │  8084   │
   └──────────┘    └──────────┘    └──────────┘    └──────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         数据存储层                                     │
│   MySQL    │    Redis    │   InfluxDB   │  Elasticsearch  │   Kafka     │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         设备接入层                                     │
│              .NET 8 网关 (5000) + EMQX MQTT Broker                   │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                      ┌──────────────────────┐
                      │   工业设备 (2000+)   │
                      └──────────────────────┘
```

### 技术栈

| 层级 | 技术选型 | 版本 |
|------|----------|------|
| 前端框架 | Vue 3 + TypeScript + Composition API | Vue 3.5 |
| 状态管理 | Pinia | 2.3 |
| UI 组件 | Element Plus | 2.9 |
| 数据可视化 | ECharts | 5.6 |
| 构建工具 | Vite | 6.0 |
| 后端框架 | Spring Boot | 3.2.5 |
| 微服务 | Spring Cloud | 2022.0.0 |
| ORM | MyBatis-Plus | 3.5.6 |
| 数据库 | MySQL | 8.0.33 |
| 缓存 | Redis | 7 |
| 消息队列 | Kafka | 3.4 |
| 时序数据库 | InfluxDB | 2.7 |
| 搜索引擎 | Elasticsearch | 8.10.0 |
| 设备接入 | .NET 8 + MQTT | .NET 8 |
| AI 推理 | Python FastAPI | 0.115 |
| 机器学习 | LightGBM + XGBoost | 4.5 / 2.1 |

---

## 项目结构

```
Smart-Factory-MES-System/
├── docs/                           # 项目文档
│   ├── DESIGN.md                  # 技术架构设计文档
│   ├── DEVELOPMENT.md             # 开发指南
│   ├── DATABASE.md                # 数据库设计
│   └── CHANGELOG.md              # 更新日志
├── mes-common/                     # 公共模块（实体类、工具类）
├── mes-gateway/                   # API 网关 (9090)
├── mes-auth/                      # 认证服务 (8081)
│   └── src/main/java/.../
│       ├── controller/            # REST API 控制器
│       ├── service/               # 业务逻辑层
│       ├── mapper/               # 数据访问层
│       └── entity/               # 实体类
├── mes-workorder/                 # 工单服务 (8082)
├── mes-process/                   # 工艺服务 (8083)
├── mes-quality/                   # 质量服务 (8084)
├── mes-dashboard/                 # 看板服务 (8085)
│   └── src/main/java/.../
│       ├── controller/
│       ├── service/impl/
│       ├── mapper/
│       ├── websocket/             # WebSocket 处理
│       └── config/                # 配置类
├── mes-device-gateway/            # .NET 设备网关 (5000)
│   └── src/MesDeviceGateway/
│       ├── Program.cs
│       ├── Controllers/           # MQTT/Kafka 控制器
│       └── Services/             # 设备接入服务
├── mes-ai-service/               # Python AI 推理服务 (8086)
│   ├── src/
│   │   ├── app.py               # FastAPI 主应用
│   │   ├── router/              # API 路由
│   │   ├── services/            # AI 推理服务
│   │   └── schemas/             # Pydantic 模型
│   ├── models/                   # ONNX 模型文件
│   └── requirements.txt
├── mes-frontend/                   # Vue 3 前端 (3000)
│   ├── src/
│   │   ├── api/                # API 请求封装
│   │   ├── views/               # 页面组件
│   │   ├── stores/               # Pinia 状态管理
│   │   ├── router/              # Vue Router 配置
│   │   ├── directives/           # 自定义指令
│   │   └── assets/               # 静态资源
│   └── package.json
├── mes-device-simulator-wpf/      # WPF 设备模拟器
├── sql/                          # 数据库脚本
│   └── init.sql                  # 初始化 SQL
├── scripts/                      # 工具脚本
├── docker-compose.yml             # 基础设施编排
├── start-all.bat                 # 一键启动脚本
└── pom.xml                      # Maven 父 POM
```

---

## 核心功能

### 1. 生产工单管理
- 工单创建、下发、执行、报工、完工
- 工单状态流转与跟踪
- 生产进度实时可视化

### 2. 工艺参数配置
- 工艺模板管理
- 参数配置与版本管理
- 实时工艺参数校验

### 3. 质量检验追溯
- 质检记录与判定
- 正反向质量追溯
- SPC 统计分析

### 4. 设备监控报警
- 设备状态实时监控
- OEE 设备综合效率计算
- 报警规则与通知

### 5. AI 智能预测
- 质量预测（准确率 95%）
- 设备故障预警（提前 24 小时）
- 工艺参数推荐
- 产能预测

### 6. 生产报表统计
- 多维度生产报表
- ECharts 可视化展示
- 数据导出功能

---

## 快速开始

### 前置条件

| 工具 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | Java 后端服务 |
| Maven | 3.9+ | 项目构建 |
| Node.js | 18+ | 前端开发 |
| Python | 3.11+ | AI 服务（可选） |
| Docker | 24+ | 基础设施容器 |
| .NET SDK | 8.0+ | 设备网关 |

> **架构说明**：Docker 仅运行基础设施（MySQL、Redis、Kafka、Zookeeper、Nacos），
> Java 后端服务在宿主机上通过 `java -jar` 直接运行。AI 服务端口 8086 与 InfluxDB 冲突，
> 启动 AI 前需执行 `docker stop mes-influxdb`。

### 启动方式

#### 方式一：一键启动（推荐）

```powershell
# 双击运行或命令行执行
start-all.bat
```

菜单选项说明：

| 选项 | 功能 |
|------|------|
| [1] Start All Services | 启动所有服务 |
| [2] Start Docker | 仅启动 MySQL/Redis/Kafka/Nacos |
| [3] Start Backend | 仅启动 Java 后端 |
| [4] Start AI Service | 仅启动 AI 预测服务 |
| [5] Start .NET Gateway | 仅启动设备网关 |
| [6] Start Frontend | 仅启动 Vue 前端 |
| [7] Start Device Simulator | 启动设备模拟器 |
| [9] Stop All Services | 停止所有服务 |

#### 方式二：Docker 启动基础设施

```powershell
# 启动基础设施（MySQL, Redis, Kafka, ZK, Nacos）
docker-compose up -d

# 查看容器状态
docker-compose ps
```

#### 方式三：手动启动

```powershell
# 1. 编译后端
mvn clean package -DskipTests

# 2. 启动基础设施
docker-compose up -d

# 3. 启动各个 Java 服务
java -jar mes-auth/target/mes-auth-1.0.0-SNAPSHOT.jar
java -jar mes-workorder/target/mes-workorder-1.0.0-SNAPSHOT.jar
java -jar mes-process/target/mes-process-1.0.0-SNAPSHOT.jar
java -jar mes-quality/target/mes-quality-1.0.0-SNAPSHOT.jar
java -jar mes-dashboard/target/mes-dashboard-1.0.0-SNAPSHOT.jar
java -jar mes-gateway/target/mes-gateway-1.0.0-SNAPSHOT.jar

# 4. 启动 AI 服务（需先停掉 InfluxDB）
docker stop mes-influxdb
cd mes-ai-service
pip install -r requirements.txt
python src/main.py

# 5. 启动前端
cd mes-frontend
npm install
npm run dev
```

---

## 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端首页 | http://localhost:3000 | Vue 3 应用 |
| API 网关 | http://localhost:9090 | Spring Cloud Gateway（已启用） |
| 认证服务 | http://localhost:8081/swagger-ui.html | Knife4j API 文档 |
| 工单服务 | http://localhost:8082/swagger-ui.html | - |
| 工艺服务 | http://localhost:8083/swagger-ui.html | - |
| 质量服务 | http://localhost:8084/swagger-ui.html | - |
| 看板服务 | http://localhost:8085/swagger-ui.html | - |
| AI 服务 | http://localhost:8086/docs | FastAPI API 文档 |
| 设备网关 | http://localhost:5000 | ASP.NET Core |
| MySQL | localhost:3306 | root/root |
| Redis | localhost:6379 | 无密码 |
| Nacos | http://localhost:8848/nacos | nacos/nacos |

### 默认账号

| 系统 | 用户名 | 密码 |
|------|--------|------|
| 前端 | admin | admin123 |
| EMQX | admin | public |

---

## API 文档

系统提供完整的 RESTful API 文档，采用 Knife4j 作为 API 文档解决方案。

### 认证服务 API

```bash
# 登录
POST /auth/login
{
  "username": "admin",
  "password": "admin123"
}

# 获取用户信息
GET /auth/userinfo
Authorization: Bearer <token>

# 获取权限列表
GET /auth/role/permissions
```

### 工单服务 API

```bash
# 工单列表
GET /workorder/page?current=1&size=10

# 创建工单
POST /workorder
{
  "workorderNo": "WO20260505001",
  "productName": "产品A",
  "quantity": 100
}
```

### 设备服务 API

```bash
# 设备列表
GET /device/page

# 设备状态
GET /device/status/{deviceId}

# 设备报警
GET /alarm/device/{deviceId}
```

完整 API 文档请访问各服务的 Swagger UI：
- 认证服务：http://localhost:8081/swagger-ui.html
- 工单服务：http://localhost:8082/swagger-ui.html
- 工艺服务：http://localhost:8083/swagger-ui.html
- 质量服务：http://localhost:8084/swagger-ui.html
- 看板服务：http://localhost:8085/swagger-ui.html
- AI 服务：http://localhost:8086/docs

---

## 配置说明

### 数据库连接配置

各服务的数据库连接在 `src/main/resources/application.yml` 中配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mes_db
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### Redis 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### Kafka 配置

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: mes-group
```

### WebSocket 配置

看板服务支持 WebSocket 实时推送：

```javascript
// 前端连接
const ws = new WebSocket('ws://localhost:8085/ws/dashboard');
ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('设备状态更新:', data);
};
```

---

## 目录说明

| 目录 | 说明 |
|------|------|
| `docs/` | 项目文档目录 |
| `mes-common/` | 公共模块，包含实体类、工具类、异常处理 |
| `mes-gateway/` | API 网关，统一路由、限流、认证 |
| `mes-auth/` | 认证服务，用户登录、Token 发放、权限管理 |
| `mes-workorder/` | 工单服务，生产工单全生命周期管理 |
| `mes-process/` | 工艺服务，工艺模板、参数配置 |
| `mes-quality/` | 质量服务，质检记录、质量追溯 |
| `mes-dashboard/` | 看板服务，实时数据展示、WebSocket 推送 |
| `mes-device-gateway/` | .NET 设备网关，MQTT 接入、Kafka 转发 |
| `mes-ai-service/` | Python AI 推理服务 |
| `mes-frontend/` | Vue 3 前端应用 |

---

## 开发指南

详细开发指南请参阅：

- [技术架构设计文档](./docs/DESIGN.md)
- [开发指南文档](./docs/DEVELOPMENT.md)
- [数据库设计文档](./docs/DATABASE.md)
- [更新日志](./docs/CHANGELOG.md)

---

## 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/xxx`)
3. 提交更改 (`git commit -m 'Add xxx'`)
4. 推送到分支 (`git push origin feature/xxx`)
5. 创建 Pull Request

---

## 更新日志

### v1.0.28 (2026-07-27)

- 基础设施 Docker-only 运行架构（Java 服务本地直接启动）
- 网关正式启用，新增 `/ai/**` 路由
- 修复 Dashboard `@RequestMapping` 前缀匹配问题
- 修复 Vite 代理 `/ai` 路径重写
- 前端 WebSocket/AI URL 改用环境变量
- Docker 内存限制及配置优化
- AI 服务 SciPy 依赖版本锁定

### v1.0.27 (2026-05-05)

- 新增生产线管理功能
- 新增工位管理功能
- 新增基础数据控制器
- 增强权限管理

### v1.0.26 (2026-05-04)

- 新增 AI 质量预测模块
- 优化设备监控 WebSocket 推送
- 完善 RBAC 权限控制
- 添加亮色/暗色主题切换

### v1.0.25 (2026-04-20)

- 微服务架构优化
- 集成 Knife4j API 文档
- 添加设备模拟器

### v1.0.1 (2024-01-01)

- 初始版本发布

完整更新日志请查看 [CHANGELOG.md](./docs/CHANGELOG.md)

---

<p align="center">Generated by AI Assistant</p>
