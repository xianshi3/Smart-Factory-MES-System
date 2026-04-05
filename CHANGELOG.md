# 更新日志 (Changelog)

## 项目概述

Smart Factory MES System - 智能工厂制造执行系统

- **创建日期**: 2026-04-04
- **技术栈**: Java (Spring Cloud) + .NET 8 + Python + Vue 3
- **架构**: 微服务架构，支持2000+设备并发连接

---

## v1.0.0 (2026-04-04)

### 新增功能

#### 1. 项目结构初始化
- 创建完整的微服务架构项目
- 7个Java微服务模块 (mes-common, mes-gateway, mes-auth, mes-workorder, mes-process, mes-quality, mes-dashboard)
- .NET 8 设备网关 (mes-device-gateway)
- Python AI 服务 (mes-ai-service)
- Vue 3 前端 (mes-frontend)

#### 2. 数据库初始化
- 创建 sql/init.sql 初始化脚本
- 包含所有业务表: users, work_orders, work_reports, process_templates, quality_records, traceability_records, production_stats
- 包含示例测试数据

#### 3. 容器化支持
- 为所有Java服务创建 Dockerfile
- 为前端创建 Dockerfile 和 nginx.conf
- 创建 .dockerignore 文件
- 创建 docker-compose.yml 和 docker-compose.dev.yml

#### 4. 配置和脚本文件
- 创建 .gitignore 文件
- 创建 parent pom.xml (统一版本管理)
- 创建各模块 pom.xml

### 错误修复

#### Java 编译错误修复 (15+处)

1. **Parent POM 引用修复**
   - 问题: 引用不存在的 "mes-parent"
   - 修复: 改为 "smart-factory-mes"

2. **UserMapper 缺失方法**
   - 问题: 缺少 selectByUsername 方法
   - 修复: 添加 @Select 注解

3. **AuthService 缺失方法**
   - 问题: 缺少 getUserInfoFromToken 方法
   - 修复: 实现该方法

4. **WorkOrderServiceImpl 方法名大小写**
   - 问题: Create → create 方法名不匹配
   - 修复: 统一方法名

5. **包导入错误**
   - 问题: com.mes.common.domain → com.mes.common.result
   - 修复: 修正导入路径

6. **Result 和 PageResult 类补全**
   - 问题: 缺少 success(), error(), of(Page) 方法
   - 修复: 添加缺失方法

7. **ProcessTemplate 版本字段类型**
   - 问题: String → Integer 类型冲突
   - 修复: 统一为 Integer

8. **泛型类型推断问题**
   - 问题: Result.success() 泛型推断失败
   - 修复: 改为 Result.ok()

9. **Traceability/QualityRecord 导入错误**
   - 问题: 使用错误的 BaseEntity 导入
   - 修复: 修正导入

10. **DashboardServiceImpl InfluxDB API**
    - 问题: API 版本不兼容
    - 修复: 简化实现

11. **ProductionStatsMapper 缺失**
    - 问题: 缺少 Mapper 接口
    - 修复: 创建完整的 Mapper

### 版本更新

根据技术规格文档更新以下依赖版本:

| 组件 | 原版本 | 新版本 |
|------|--------|--------|
| Spring Cloud | 2023.0.1 | 2022.0.0 |
| Spring Cloud Alibaba | 2023.0.1.0 | 2022.0.0.0 |
| MySQL | 8.3.0 | 8.0.33 |
| Kafka | 3.7.0 | 3.4.0 |
| Elasticsearch | 8.13.2 | 8.10.0 |
| Seata | 1.8.0 | 1.6.1 |
| InfluxDB Client | - | 7.1.0 (provided) |
| ONNX Runtime | - | 1.15.1 |
| EMQX | - | 5.8.0 |

### 构建验证

- ✅ 所有7个Java模块编译成功 (BUILD SUCCESS)
- ✅ 前端项目构建成功
- ✅ 无编译错误

### v1.0.2 (2026-04-04)

#### 新增功能

1. **快速启动脚本**
   - 新增 `start-docker.bat` - 一键启动 MySQL 和 Redis，自动初始化数据库
   - 新增 `stop-docker.bat` - 一键停止 Docker 容器
   - 新增 `start-backend.bat` - 一键编译打包并启动所有后端服务
   - 新增 `stop-backend.bat` - 一键停止所有后端服务
   - 简化开发工作流

#### 错误修复

1. **MySQL 8.0.33 配置兼容性问题**
   - 问题：`--default-auth-plugin=mysql_native_password` 参数不兼容
   - 修复：移除该参数

2. **数据库初始化脚本重复字段**
   - 问题：`proc_template` 表有两个 `version` 字段
   - 修复：删除重复的版本号字段

3. **网关路由配置问题**
   - 问题：使用 lb:// 负载均衡需要 Nacos，移除 Nacos 依赖后无法工作
   - 修复：改为直接 HTTP 路由 `http://localhost:port`

4. **Spring Cloud 版本不兼容**
   - 问题：Spring Boot 3.2.5 与 Spring Cloud 2022.0.0 不兼容
   - 修复：移除 spring-cloud-starter-bootstrap 依赖，禁用兼容性检查

5. **移除不必要的外部依赖**
   - 移除所有服务的 Nacos 配置（discovery, config）
   - 移除 Seata 分布式事务配置
   - 移除 Kafka、Elasticsearch、InfluxDB 配置（开发环境不需要）

6. **JwtUtils Bean 扫描问题**
   - 问题：mes-common 模块的 JwtUtils 无法被 mes-auth 扫描到
   - 修复：在 AuthApplication 添加 @ComponentScan({"com.mes.auth", "com.mes.common"})

#### 文档更新
- README.md - 添加快速启动脚本说明
- DEVELOPMENT.md - 添加快速启动脚本使用说明

---

### v1.0.1 (2026-04-04)

#### 新增功能

#### 5. 设备模拟器 (mes-device-simulator)
- 功能: 模拟2000+设备通过MQTT上报数据
- 技术: Node.js + mqtt.js
- 配置: .env 文件可调整设备数量和上报间隔
- 目录: mes-device-simulator/
- 特性:
  - 支持可配置设备数量（默认2000）
  - 模拟设备参数：温度、压力、速度、振动、功率、运行时长
  - 随机生成设备状态变化（ONLINE/OFFLINE/ALARM/MAINTENANCE）
  - 批量上报优化，支持高并发测试

#### 6. ONNX模型文件
- 创建: scripts/generate_models.py
- 模型: mes-ai-service/src/models/saved_models/
  - quality_predict.onnx (132 bytes) - 质量预测
  - output_predict.onnx (132 bytes) - 产量预测
- 注意: 示例模型，实际使用需用真实数据训练

#### 7. 启动指南更新
- 新增 4.7 设备模拟器启动说明
- 新增 4.8 快速启动（Docker Compose）
- 新增 4.9 服务验证检查表
- 更新服务端口映射（新增mes-device-simulator）

#### 8. 代码注释
- 为所有Java文件添加Javadoc注释（类、方法、字段）
- 为C#文件添加XML文档注释
- 为Python文件添加Docstring注释
- 为Vue/TypeScript文件添加JSDoc注释
- 为JavaScript文件添加JSDoc注释
- 总计：约116个代码文件

#### 9. 前端配置修复
- 修复 Sass @import 弃用警告，改用 @use
- 修复 TypeScript baseUrl 弃用警告，添加 ignoreDeprecations

#### 10. 文档更新
- 新增 README.md 项目简介
- 添加默认账号说明（admin/admin123, nacos/nacos, admin/public）
- 更新 docker 命令为 docker compose

#### 11. Docker配置优化
- 简化 docker-compose.yml，默认只启动 MySQL + Redis（开发友好）
- 解决 Docker Hub 镜像拉取失败问题（VPN方案）

---

## 文档文件

- `DESIGN.md` - 技术设计文档 (中文内容)
- `DEVELOPMENT.md` - 开发文档 (中文内容)
- `CHANGELOG.md` - 更新日志 (本文件)

---

## 目录结构

```
Smart-Factory-MES-System/
├── .gitignore
├── pom.xml (Parent POM)
├── docker-compose.yml
├── docker-compose.dev.yml
├── sql/init.sql
├── scripts/                   (工具脚本)
│   └── generate_models.py    (ONNX模型生成)
├── DESIGN.md
├── DEVELOPMENT.md
├── CHANGELOG.md
├── mes-common/            (公共模块)
├── mes-gateway/           (网关服务)
├── mes-auth/              (认证服务)
├── mes-workorder/         (工单服务)
├── mes-process/           (工艺服务)
├── mes-quality/            (质量服务)
├── mes-dashboard/         (仪表板服务)
├── mes-device-gateway/    (.NET 设备网关)
├── mes-ai-service/        (Python AI 服务)
├── mes-device-simulator/  (设备模拟器)
└── mes-frontend/         (Vue 3 前端)
```

---

## v1.0.10 (2026-04-05)

### 文档清理

1. **删除冗余文件**
   - 删除 `DEVELOPMENT.html` (27KB)
   - 保留 `DEVELOPMENT.md` 作为源文件

### 数据库更新

1. **完善设备数据**
   - 将4条设备记录扩展为12条
   - 新增字段：temperature (温度)、speed (速度)、last_heartbeat (心跳时间)
   - 涵盖多种设备类型：CNC加工中心、自动组装线、质量检测台、阳极氧化线、喷涂工作站、激光刻蚀机、包装流水线
   - 包含不同状态：ONLINE、OFFLINE、MAINTENANCE、ALARM

### OpenCode配置

1. **新增配置文件**
   - 创建 `opencode.json` - 项目级配置
   - 创建 `.opencode/instructions.md` - 指令文件
   - 配置项：提交Git前必须先确认

### 文件更新

1. `mes-device-gateway/src/MesDeviceGateway/Services/KafkaProducerService.cs`
2. `mes-device-gateway/src/MesDeviceGateway/Services/MqttConsumerService.cs`
3. `mes-device-gateway/src/MesDeviceGateway/Config/GatewayConfig.cs`
4. `mes-device-gateway/src/MesDeviceGateway/Extensions/ServiceCollectionExtensions.cs`
5. `mes-device-gateway/src/MesDeviceGateway/Program.cs`
6. `mes-device-gateway/src/MesDeviceGateway/appsettings.json`

---

## v1.0.12 (2026-04-05)

### AI服务启动

1. **AI服务部署**
   - 端口 8086
   - 启动脚本: start-ai.bat
   - 停止脚本: stop-ai.bat

2. **前端AI预测功能**
   - 设备监控页面添加"AI预测"按钮
   - 调用质量预测API

3. **CORS配置**
   - AI服务添加跨域支持

### 文件更新

1. `mes-ai-service/src/app.py` - 添加CORS
2. `mes-frontend/src/api/services.ts` - 添加AI API
3. `mes-frontend/src/views/device/DeviceView.vue` - AI预测功能
4. `start-ai.bat` - 启动脚本
5. `stop-ai.bat` - 停止脚本

---

## v1.0.13 (2026-04-05)

### 前端优化

1. **设备页面数据对接**
   - 图表使用真实设备数据
   - 告警列表使用真实数据

2. **统一消息弹窗**
   - 新增 `src/utils/message.ts` 工具函数
   - 错误/成功/警告/确认弹窗统一管理

3. **弹窗样式美化**
   - 暗色主题弹窗样式
   - 圆角按钮
   - 居中布局

4. **错误处理优化**
   - 移除全局错误弹窗
   - 单次弹窗+控制台日志
   - 详细错误信息展示

### 文件更新

1. `mes-frontend/src/utils/message.ts` - 新增消息工具
2. `mes-frontend/src/styles/index.scss` - 弹窗样式
3. `mes-frontend/src/views/device/DeviceView.vue` - 真实数据
4. `mes-dashboard/controller/DashboardController.java` - 告警接口

### v1.0.13 (2026-04-05)
- 设备页面图表和告警使用真实数据
- AI预测修复：无模型时返回默认值
- 清理日志文件

### 文件更新

1. `mes-ai-service/src/models/prediction_model.py` - 修复预测
2. `start-ai.bat` - 修复启动脚本

---

*Generated by AI Assistant - 2026-04-05*

---

## v1.0.4 (2026-04-05)

### 配置更新

1. **网关端口变更**
   - 原因: 端口8080被其他进程占用
   - 修复: 将网关端口从8080改为9090
   - 文件: mes-gateway/src/main/resources/application.yml

2. **Docker容器名称冲突**
   - 问题: 重复运行start-docker.bat导致容器名称冲突
   - 修复: 改为唯一名称 mes-mysql-dev / mes-redis-dev
   - 文件: docker-compose.yml, start-docker.bat

3. **前端API配置更新**
   - 更新: VITE_API_BASE_URL=http://localhost:9090/api
   - 文件: mes-frontend/.env.development

### 文档更新

- README.md - 网关端口更新为9090，移除Nacos账号
- start-backend.bat - 网关端口更新为9090
- start-docker.bat - 容器名称更新，添加先停止旧容器

### 当前服务状态

| 服务 | 端口 | 状态 |
|------|------|------|
| Frontend (Vue) | 3000 | 运行中 |
| Gateway | 9090 | 运行中 |
| Auth | 8081 | 运行中 |
| WorkOrder | 8082 | 运行中 |
| Process | 8083 | 运行中 |
| Quality | 8084 | 运行中 |
| Dashboard | 8085 | 运行中 |
| MySQL | 3306 | 运行中 |
| Redis | 6379 | 运行中 |

### 访问信息

- 前端地址: http://localhost:3000
- 网关地址: http://localhost:9090
- 登录账号: admin / admin123
- 数据库: mes_db (root/root)

---

## v1.0.5 (2026-04-05)

### 配置更新

1. **前端直连Auth服务**
   - 原因: Gateway存在路由问题，暂时无法正常工作
   - 修复: 前端直接连接 Auth 服务 8081 端口
   - 文件: mes-frontend/.env.development

2. **Docker容器名称统一**
   - 容器名: mes-mysql / mes-redis
   - 文件: docker-compose.yml (恢复原名称)

### 当前服务状态

| 服务 | 端口 | 状态 |
|------|------|------|
| Frontend (Vue) | 3000 | 运行中 |
| Gateway | 9090 | 运行中 |
| Auth | 8081 | 运行中 |
| WorkOrder | 8082 | 运行中 |
| Process | 8083 | 运行中 |
| Quality | 8084 | 运行中 |
| Dashboard | 8085 | 未启动 |
| MySQL | 3306 | 运行中 |
| Redis | 6379 | 运行中 |

### 访问信息

- 前端地址: http://localhost:3000
- 登录账号: admin / admin123
- 数据库: mes_db (root/root)

---

## v1.0.6 (2026-04-05)

### 问题描述

前端所有API请求都发到了Auth服务(8081)，但各微服务有独立端口，导致以下错误：
- `No static resource quality/record/page`
- `No static resource workorder/page`
- `No static resource process/template/page`

### 解决方案

创建 `services.ts` 统一管理各服务API地址，每个API使用完整URL：
- WorkOrder: http://localhost:8082
- Process: http://localhost:8083
- Quality: http://localhost:8084

### 文件更新

1. **新建文件**
   - `mes-frontend/src/api/services.ts` - 统一管理所有服务API地址

2. **修改文件**
   - `mes-frontend/src/api/index.ts` - token获取改为localStorage
   - `mes-frontend/src/views/workorder/WorkOrderView.vue` - import from `@api/services`
   - `mes-frontend/src/views/process/ProcessView.vue` - import from `@api/services`
   - `mes-frontend/src/views/quality/QualityView.vue` - import from `@api/services`

### 当前服务状态

| 服务 | 端口 |
|------|------|
| Frontend (Vue) | 3000 |
| Auth | 8081 |
| WorkOrder | 8082 |
| Process | 8083 |
| Quality | 8084 |
| Dashboard | 8085 |
| MySQL | 3306 |
| Redis | 6379 |

---

## v1.0.7 (2026-04-05)

### 问题诊断

1. **POM配置缺失**：`mes-workorder` 和 `mes-quality` 缺少 `spring-boot-maven-plugin`，导致打包的 JAR 无法独立运行
2. **Quality服务路径错误**：后端使用 `/quality/record/page`，前端直接调用 `/record/page`

### 解决方案

1. 修复 `mes-workorder/pom.xml` - 添加 spring-boot-maven-plugin
2. 修复 `mes-quality/pom.xml` - 添加 spring-boot-maven-plugin
3. 修复 `services.ts` - Quality服务URL改为 `http://localhost:8084/quality`

### 服务验证

| 服务 | 端口 | API测试 |
|------|------|---------|
| Frontend | 3000 | ✅ 200 |
| Auth | 8081 | ✅ 200 |
| WorkOrder | 8082 | ✅ /workorder/page |
| Process | 8083 | ✅ /template/page |
| Quality | 8084 | ✅ /quality/record/page |

---

## v1.0.8 (2026-04-05)

### 问题描述

前端页面显示 "请求失败 (code: undefined)" 和 "服务器内部错误"

### 问题诊断

1. **CORS跨域配置缺失**：workorder、process、quality 服务没有配置跨域，前端无法直接调用
2. **API响应处理问题**：前端无法正确处理非标准响应格式

### 解决方案

1. **添加CORS配置** - 为三个服务添加跨域配置：
   - `mes-workorder/config/WebMvcConfig.java`
   - `mes-process/config/WebMvcConfig.java`
   - `mes-quality/config/WebMvcConfig.java`

2. **修复前端API响应处理** - `mes-frontend/src/api/index.ts`：
   - 处理 `code: undefined` 情况
   - 增强对非标准响应的兼容性

### 文件更新

1. **新建文件**
   - `mes-workorder/src/main/java/com/mes/workorder/config/WebMvcConfig.java`
   - `mes-process/src/main/java/com/mes/process/config/WebMvcConfig.java`
   - `mes-quality/src/main/java/com/mes/quality/config/WebMvcConfig.java`

2. **修改文件**
   - `mes-frontend/src/api/index.ts` - 响应拦截器优化

### 当前服务状态

| 服务 | 端口 | 状态 |
|------|------|------|
| Frontend | 3000 | 运行中 |
| Auth | 8081 | 运行中 |
| WorkOrder | 8082 | 运行中 |
| Process | 8083 | 运行中 |
| Quality | 8084 | 运行中 |
| MySQL | 3306 | Docker |
| Redis | 6379 | Docker |

### 访问信息

- 前端地址: http://localhost:3000
- 登录账号: admin / admin123
- 数据库: mes_db (root/root)

---

## v1.0.9 (2026-04-05)

### .NET 设备网关优化

1. **KafkaProducerService 优化**
   - 使用 Channel<T> 实现高吞吐量异步消息处理
   - 替代原来的 ConcurrentQueue + Timer 方案

2. **MqttConsumerService 优化**
   - 添加自动重连机制
   - Channel 消息队列缓冲
   - 错误恢复机制

3. **GatewayConfig 新增配置**
   - MqttReconnectIntervalMs: MQTT重连间隔
   - ChannelBufferSize: 消息通道缓冲区大小
   - EnableIdempotent: Kafka幂等生产者

4. **ServiceCollectionExtensions 优化**
   - 添加健康检查支持
   - TCP端口健康检查

5. **Program.cs 优化**
   - 结构化日志 (Serilog)
   - 健康检查端点

### 文件更新

1. `mes-device-gateway/src/MesDeviceGateway/Services/KafkaProducerService.cs`
2. `mes-device-gateway/src/MesDeviceGateway/Services/MqttConsumerService.cs`
3. `mes-device-gateway/src/MesDeviceGateway/Config/GatewayConfig.cs`
4. `mes-device-gateway/src/MesDeviceGateway/Extensions/ServiceCollectionExtensions.cs`
5. `mes-device-gateway/src/MesDeviceGateway/Program.cs`
6. `mes-device-gateway/src/MesDeviceGateway/appsettings.json`