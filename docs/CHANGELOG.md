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

### 统一启动器

1. **start-all.bat**
   - 启动所有服务一键启动
   - 检测已运行服务自动跳过
   - 彩色状态表格显示

2. **新增服务**
   - start-gateway-dotnet.bat (.NET设备网关)

---

## v1.0.18 (2026-04-06)

### 启动器修复

1. **start-all.bat 路径问题修复**
   - 修复路径包含空格导致命令执行失败
   - 使用 `"%ROOT%\xxx"` 格式正确引用路径
   - 修复后端服务启动命令格式

2. **菜单选项完善**
   - [1] Start All Services - 启动所有服务
   - [2] Start Docker - 启动 MySQL/Redis
   - [3] Start Backend - 启动 Java 后端
   - [4] Start AI Service - 启动 Python AI
   - [5] Start .NET Gateway - 启动设备网关
   - [6] Start Frontend - 启动 Vue 前端
   - [7] Start Device Simulator - 启动设备模拟器
   - [8] Clean - 清理缓存文件
   - [9] Stop All Services - 停止所有服务
   - [10] View Status - 查看服务状态

3. **AI 服务启动优化**
   - 修复虚拟环境激活问题
   - 直接调用 `.venv\Scripts\python.exe`

4. **停止和状态功能完善**
   - stop_all 标签实现
   - status 标签实现（检测所有服务端口）

5. **清理功能**
   - 新增 clean.bat 脚本
   - 清理 .opencode 文件夹
   - 清理 Python 缓存文件
   - 清理 Java target 文件（保留 JAR）
   - 清理 .NET obj/bin 文件
   - 清理 .idea 文件夹

### 文件更新

1. `start-all.bat` - 启动器完整重写
2. `clean.bat` - 清理脚本
3. `README.md` - 更新启动说明
4. `DEVELOPMENT.md` - 更新菜单选项

---

## v1.0.17 (2026-04-05)

### 前端修复

1. **Vite 代理配置**
   - 修复 `/process` 路径重写问题
   - 解决刷新页面 405 错误

2. **状态值统一**
   - 工艺管理：DRAFT/PUBLISHED
   - 质量管理：PASSED/FAILED/REWORK
   - 修复前端与后端状态值不匹配

3. **API 参数对齐**
   - 工艺查询使用 `keyword` 参数
   - 质量查询使用 `keyword` 参数

### 后端修复

1. **MyBatis-Plus 分页插件**
   - 添加 `MybatisPlusConfig` 到 Process 服务
   - 修复 `total=0` 问题

2. **空字符串过滤**
   - 修复 QualityService 空字符串导致查询异常

### 文件更新

1. `mes-frontend/vite.config.ts`
2. `mes-frontend/src/views/process/ProcessView.vue`
3. `mes-frontend/src/views/quality/QualityView.vue`
4. `mes-process/src/main/java/com/mes/process/config/MybatisPlusConfig.java`
5. `mes-process/src/main/java/com/mes/process/controller/ProcessController.java`
6. `mes-quality/src/main/java/com/mes/quality/service/impl/QualityServiceImpl.java`

---

## v1.0.16 (2026-04-05)

### 前端修复

1. **API 请求路由修复**
   - 修复 `.env.development` 硬编码 8081 问题
   - 所有请求使用相对路径，由 Vite 代理路由
   - 代理配置：workorder→8082, process→8083, quality→8084, api→8085

2. **services.ts 优化**
   - 移除所有硬编码 URL
   - 使用相对路径（如 `/workorder/page`）

3. **DashboardView 优化**
   - 对接真实设备 API
   - 图表数据动态更新

### 文件更新

1. `mes-frontend/.env.development` - 修复 baseURL
2. `mes-frontend/vite.config.ts` - 多服务代理配置
3. `mes-frontend/src/api/services.ts` - 相对路径
4. `mes-frontend/src/views/dashboard/DashboardView.vue` - 真实数据

---

## v1.0.15 (2026-04-05)

### 安全修复

1. **密码加密**
   - 添加 Spring Security BCrypt 加密
   - 登录/注册使用密码哈希验证
   - 数据库种子数据密码改为哈希值

2. **JWT 密钥管理**
   - 移除 JWT 密钥硬编码默认值
   - 强制从环境变量读取

### 数据逻辑修复

1. **Dashboard 服务**
   - 修复概览数据查询真实生产统计
   - OEE 计算从数据库读取实际数据
   - 趋势数据填充每日生产统计
   - Redis 缓存 JSON 序列化修复 (Jackson)

### Python AI 服务修复

1. **Bug 修复**
   - 修复 `inference_service.py` 中 `config` 变量名错误
   - 修复 `import os` 位置错误 (移到文件顶部)

### .NET 设备网关修复

1. **MQTT Bug 修复**
   - 修复 Guid 格式化异常 (`:N[..8]` -> `ToString("N").Substring(0, 8)`)
   - 网关可在无 Kafka/MQTT 环境下正常启动

### 启动器优化

1. **统一启动器**
   - 使用 `/D` 参数修复 .NET 网关启动路径问题
   - 清理冗余 bat 文件
   - 支持一键启动/停止所有服务

### 文件更新

1. `mes-auth/pom.xml` - 添加 spring-security-crypto
2. `mes-auth/src/.../AuthService.java` - BCrypt 加密
3. `sql/init.sql` - 密码哈希化
4. `mes-dashboard/.../DashboardServiceImpl.java` - 数据逻辑修复
5. `mes-ai-service/src/models/prediction_model.py` - import 修复
6. `mes-ai-service/src/models/regression_model.py` - import 修复
7. `mes-ai-service/src/services/inference_service.py` - config 变量修复
8. `mes-device-gateway/.../MqttConsumerService.cs` - Guid 格式化修复
9. `start-all.bat` - 启动器优化

---

## v1.0.14 (2026-04-05)

### 统一启动器优化

1. **start-all.bat 完善**
   - 使用 `/D` 参数正确指定工作目录
   - 修复 .NET 网关启动路径问题
   - 支持一键启动/停止所有服务
   - 服务状态查看功能

2. **清理冗余脚本**
   - 删除所有独立 bat 文件
   - 统一使用 start-all.bat

### .NET 设备网关修复

1. **MQTT Bug 修复**
   - 修复 Guid 格式化异常
   - 使用 `ToString("N").Substring(0, 8)` 替代 `:N[..8]`

2. **Kafka 配置调整**
   - 禁用幂等模式（适配无 Kafka 开发环境）
   - 网关可在无 Kafka/MQTT 环境下正常启动

### 文件更新

1. `start-all.bat` - 启动器优化
2. `mes-device-gateway/src/MesDeviceGateway/Services/MqttConsumerService.cs` - MQTT Bug 修复
3. `mes-device-gateway/src/MesDeviceGateway/Services/KafkaProducerService.cs` - Kafka 配置调整

---

*Generated by AI Assistant - 2026-04-12*

---

## v1.0.21 (2026-04-12)

### 功能新增

#### 1. 工单完工功能
- **后端**: `WorkOrderService.complete()` 方法
- **接口**: `POST /workorder/{id}/complete`
- **前端**: 工单页面添加"完成"按钮

#### 2. 设备实时数据流
- **WebSocket**: 设备页面集成 WebSocket，每5秒自动更新
- **端点**: `ws://localhost:8085/ws/dashboard`

#### 3. 设备控制功能
- **后端**: `DashboardService.startDevice()` / `stopDevice()`
- **接口**: `POST /api/dashboard/device/{id}/start` 和 `/stop`
- **前端**: 设备页面添加"启动"/"停止"按钮

#### 4. 生产报表功能
- **后端**: `DashboardService.getProductionReport()` 
- **接口**: `GET /api/dashboard/report/production`
- **前端**: 新增 `views/report/ReportView.vue` 报表页面
- **功能**: 总产量、良品率、平均OEE、日趋势图表

### 文件更新

| 文件 | 说明 |
|------|------|
| `mes-workorder/.../WorkOrderService.java` | 添加 complete 方法 |
| `mes-workorder/.../WorkOrderServiceImpl.java` | 实现 complete 逻辑 |
| `mes-workorder/.../WorkOrderController.java` | 添加 complete 接口 |
| `mes-dashboard/.../DashboardController.java` | 设备控制+报表接口 |
| `mes-dashboard/.../DashboardService.java` | 新方法声明 |
| `mes-dashboard/.../DashboardServiceImpl.java` | 实现设备控制和报表 |
| `mes-dashboard/.../WebSocketConfig.java` | WebSocket 配置 |
| `mes-frontend/src/api/services.ts` | 新增 API 函数 |
| `mes-frontend/src/views/workorder/WorkOrderView.vue` | 完成按钮 |
| `mes-frontend/src/views/device/DeviceView.vue` | 启动/停止按钮+WebSocket |
| `mes-frontend/src/views/report/ReportView.vue` | **新增** 报表页面 |
| `mes-frontend/src/router/index.ts` | 添加报表路由 |
| `mes-frontend/src/views/layout/MainLayout.vue` | 添加报表菜单 |
| `mes-frontend/src/utils/websocket.ts` | WebSocket 服务 |
| `scripts/train_models.py` | AI模型训练脚本 |

---

## v1.0.20 (2026-04-12)

### 功能新增

#### 1. WebSocket 实时推送
- **后端**: 新增 `WebSocketConfig.java` 配置类
- **前端**: 新增 `utils/websocket.ts` WebSocket 服务封装
- **功能**: 看板页面每5秒自动接收最新设备数据和概览数据
- **端点**: `ws://localhost:8085/ws/dashboard`

#### 2. 报工提交功能
- **前端**: 工单管理页面添加"报工"按钮和对话框
- **功能**: 生产中的工单可提交报工，记录良品/不良数量
- **字段**: workOrderId, reportQuantity, qualifiedQuantity, defectiveQuantity, deviceId, remark

#### 3. 质量追溯功能
- **后端**: 已有完整实现 (forwardTrace, reverseTrace)
- **前端**: 质量页面已有追溯对话框展示

#### 4. AI模型训练脚本
- **新增**: `scripts/train_models.py` 模型训练脚本
- **功能**: 生成真实LightGBM模型，用于质量预测和产量预测
- **使用**: `python scripts/train_models.py`

### 文件更新

| 文件 | 说明 |
|------|------|
| `mes-dashboard/.../WebSocketConfig.java` | 新增WebSocket配置 |
| `mes-frontend/src/utils/websocket.ts` | 新增WebSocket封装 |
| `mes-frontend/src/views/dashboard/DashboardView.vue` | 集成WebSocket |
| `mes-frontend/src/views/workorder/WorkOrderView.vue` | 添加报工功能 |
| `scripts/train_models.py` | 新增模型训练脚本 |

---

## v1.0.19 (2026-04-12)

### 问题描述

使用 MyBatis-Plus 逻辑删除 (`@TableLogic`) 时，创建新记录报错：
```
Duplicate entry '123' for key 'proc_template.uk_template_code'
```

### 问题根因

1. 数据库唯一约束只关注单列 `template_code`，不考虑 `deleted` 状态
2. 软删除的记录 (`deleted=1`) 仍然占据唯一索引，导致相同编码无法新增
3. 所有使用逻辑删除的表都存在此问题

### 解决方案

#### 1. 修改数据库唯一约束（推荐）

将单列唯一索引改为复合索引 `(column, deleted)`：

```sql
-- 修复前
ALTER TABLE proc_template ADD UNIQUE INDEX uk_template_code (template_code);

-- 修复后：允许一个 active + 多个 deleted 记录共存
ALTER TABLE proc_template ADD UNIQUE INDEX uk_template_code (template_code, deleted);
```

#### 2. 代码层面预检（辅助）

在 Service 层添加唯一性校验：

```java
Long count = processTemplateMapper.selectCount(
    new LambdaQueryWrapper<ProcessTemplate>()
        .eq(ProcessTemplate::getTemplateCode, dto.getTemplateCode())
        .last("AND (deleted = 0 OR deleted IS NULL)")
);
if (count > 0) {
    throw new RuntimeException("模板编码已存在");
}
```

### 批量修复的表

| 表名 | 字段 | 修复前 | 修复后 |
|------|------|--------|--------|
| `wo_work_order` | order_no | order_no | order_no, deleted |
| `dash_device_status` | device_code | device_code | device_code, deleted |
| `mes_production_line` | line_code | line_code | line_code, deleted |
| `mes_workstation` | workstation_code | workstation_code | workstation_code, deleted |
| `proc_template` | template_code | template_code | template_code, deleted |

### 文件更新

1. `.gitignore` - 添加中文注释，优化结构
2. `docs/DEVELOPMENT.md` - 更新最后更新日期
3. `docs/CHANGELOG.md` - 添加 v1.0.19 更新日志
4. `docs/MyBatis-Plus-逻辑删除与唯一约束冲突问题.md` - 新增技术博客文章

---

## v1.0.10 (2026-04-10)

### 核心问题修复

#### 1. JavaScript Long ID 精度丢失
- **问题**: 后端 Long 类型 ID 超过 2^53-1 时精度丢失，导致删除失败
- **原因**: JavaScript Number 安全整数范围为 9007199254740991
- **解决方案**: 
  - 后端 BaseEntity 添加 `@JsonSerialize(using = ToStringSerializer.class)`
  - 前端 API 接受 string | number 类型

#### 2. 乐观锁 @Version 更新失败
- **问题**: 使用 updateById() 触发 @Version 乐观锁时报错
- **错误**: `Parameter 'MP_OPTLOCK_VERSION_ORIGINAL' not found`
- **解决方案**: 使用 UpdateWrapper 代替 updateById()

#### 3. 前后端字段名不匹配
- **问题**: 前端字段名与后端 DTO 不一致
- **示例**: result vs checkResult, workOrderCode vs workOrderNo
- **解决方案**: 统一字段名

### 详情对话框功能

#### 前端页面
- 工单管理：添加详情对话框，使用el-descriptions展示
- 工艺模板：添加详情对话框
- 质量管理：已有追溯功能
- 设备管理：添加详情对话框

#### 错误处理优化
- 工艺模板编辑：已发布模板不能直接修改，添加前端提示
- 保存失败时显示具体错误原因

### 数据库修复

#### 中文乱码修复
- **问题**: MySQL连接使用latin1字符集
- **解决**: 使用 `--default-character-set=utf8mb4` 连接
- **修复数据**:
  - proc_template: CNC加工工艺、测试模板工艺等
  - wo_work_order: 智能手机外壳、智能手表表壳等
  - qms_quality_record: 外观缺陷、产品表面有划痕

### 质量管理模块完善

#### 1. 创建功能
- 后端: QualityService.createRecord() 已实现
- 前端: 添加完整创建对话框表单

#### 2. 删除功能
- 后端: 添加 DELETE /quality/record/{id} 接口
- 前端: 添加删除按钮和处理函数

### 文件修改清单

| 文件 | 修改内容 |
|-----|---------|
| `mes-common/.../BaseEntity.java` | 添加 ToStringSerializer |
| `mes-frontend/.../services.ts` | 接受 string 类型 ID |
| `mes-workorder/.../WorkOrderServiceImpl.java` | 使用 UpdateWrapper |
| `mes-process/.../ProcessTemplateServiceImpl.java` | 使用 UpdateWrapper |
| `mes-quality/.../QualityService.java` | 添加 deleteRecord 方法 |
| `mes-quality/.../QualityServiceImpl.java` | 实现删除逻辑 |
| `mes-quality/.../QualityController.java` | 添加 DELETE 接口 |
| `mes-quality/.../QualityRecord.java` | 添加 ToStringSerializer |
| `mes-frontend/.../QualityView.vue` | 添加创建表单和删除按钮 |

### 文档更新

| 文件 | 说明 |
|-----|------|
| docs/问题修复记录.md | 修复问题清单 |
| docs/DEVELOPMENT.md | 添加核心实现模式 |
| docs/DATABASE.md | 数据库设计文档 |
| docs/JavaScript-Long-ID精度丢失问题最佳实践.md | 通用解决方案 |
| docs/博客文章-为什么你的删除功能突然失效了.md | 博客文章 |

### 数据库状态

- qms_quality_record: 11 条记录
- 所有业务表已添加 deleted_time, deleted_by 字段

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