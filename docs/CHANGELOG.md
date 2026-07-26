# 更新日志 (Changelog)

## 项目概述

Smart Factory MES System - 智能工厂制造执行系统

- **创建日期**: 2026-04-04
- **技术栈**: Java (Spring Cloud) + .NET 8 + Python + Vue 3 + WPF
- **架构**: 微服务架构，支持2000+设备并发连接

---

## v1.0.29 (2026-07-27)

### 文档更新

- 更新 README.md、DEVELOPMENT.md、DATABASE.md、DOC_DB_VERSION.md 至最新项目状态
- 移除不存在的 DESIGN.md 引用
- 更新数据库迁移记录至 V5

---

## v1.0.28 (2026-07-27)

### 架构调整

- **Docker 仅运行基础设施**：MySQL、Redis、Kafka、Zookeeper、Nacos 使用 Docker，Java 服务在宿主机通过 `java -jar` 运行
- **Gateway 正式启用**：路由 `/api/**` (StripPrefix=1) 和 `/api/ai/**` (StripPrefix=2) 均有效
- **前端 Vite 代理**：`/ai` → `localhost:8086` (rewrite strip `/ai`)；`/api` → `localhost:9090`；新增 `/dashboard` → `localhost:8085`
- **AI 服务端口冲突**：端口 8086 与 InfluxDB 冲突，启动前需 `docker stop mes-influxdb`
- **SciPy 锁定 1.14.1**：以兼容 NumPy 1.26.4

### Bug Fixes

#### 第一批（commit 484d352）

- **Dockerfile COPY 路径**：所有 Dockerfile 修正为正确上下文路径
- **docker-compose.yml**：新增 Zookeeper、Kafka、Nacos、InfluxDB；设置内存限制（mysql: 512M, kafka: 512M, redis: 128M, zk: 128M）
- **WebSocket URL**：前端改用 `import.meta.env.VITE_WS_URL` 环境变量
- **Vite `/ai` 代理**：修复 rewrite 剥离前缀
- **Dashboard `@RequestMapping`**：从 `/api/dashboard` 改为 `/dashboard` 以匹配 Gateway StripPrefix=1
- **Gateway 路由**：新增 `/ai/**` 路由，创建 `application-docker.yml`
- **nginx 代理**：修复 `api` 请求代理到 Gateway :9090
- **userStore 导入路径**：修正 `permission.ts` 中的导入错误
- **`env.d.ts` 移除**：替换为 `src/vite-env.d.ts`
- **`request.ts` 移除**：死代码替换为 `src/api/index.ts`
- **`package-lock.json` 追踪**：`.gitignore` 更新为保留版本控制
- **SQL 迁移命名**：`V5__fix_unique_constraint.sql` 重命名为 `V5_5__fix_unique_constraint.sql`

#### 第二批（commit 4a70d94）

- **`docker-compose.dev.yml`**：新增 `mes-auth` 服务（之前缺失）
- **网络连通性**：`docker-compose.yml` 中所有基础设施服务加入 `mes-network`；Kafka 添加 `depends_on: zookeeper`
- **ProcessController 路径**：`@RequestMapping("/template")` → `/process/template`，匹配 Gateway StripPrefix=1
- **auth mapper-locations**：修正缩进，合并两个 `classpath` 路径到同一 key
- **Gateway AI 路由**：路径从 `/ai/**` → `/api/ai/**`，StripPrefix 0 → 2，正确转发到 AI 服务 `/api/v1/…`
- **AI 路由双配置**：同时添加到 `application-docker.yml`
- **前端 AI_BASE_URL**：默认值从 `/ai` 改为 `/api/ai`
- **nginx 代理**：`/api/ai/…` 通过 `/api` 规则→Gateway→AI 服务，生产环境链路完整
- **Vite `/dashboard` 代理**：新增规则解决 `dashboard.ts` API 调用 404
- **移除重复 `getDeviceStatus`**：`services.ts` 中的重复函数已删除
- **start-all.bat**：AI 服务工作目录 `mes-ai-service\src` → `mes-ai-service`；新增 Gateway 启动/停止/状态；路径改为 `%~dp0`
- **安全**：移除 `mes-ai-service/.env.local` 中真实的 API key
- **堆栈泄露**：5 个服务的 `application.yml` 移除 `include-stacktrace: always` 和 `include-exception: true`
- **`wsConnected` 生命周期**：WebSocket `onopen` 时设置 `wsConnected = true`
- **protobuf 依赖**：添加到 `requirements.txt`
- **AI 模型文件**：移除 unused `import os`
- **GlobalExceptionHandler**：移除重复的 `HttpServletRequest` 导入
- **DashboardServiceImpl**：修正过时注释
- **docker-compose-minimal.yml**：移除废弃的 `version: '3.8'`

### Known Issues

- **Docker proxy blocks image pulls**：已在 daemon.json 配置镜像加速器作为临时方案。`openjdk:17-jdk-slim` 和 `seataio/seata-server` 无法通过 Docker 拉取。

---

## v1.0.27 (2026-05-05)

### 新增功能

#### 1. 生产线管理功能

- 新增 ProductionLine 实体类 (mes-common/entity/ProductionLine.java)
- 新增 ProductionLineMapper 接口 (mes-common/mapper/ProductionLineMapper.java)
- 新增生产线管理前端页面 (mes-frontend/src/views/production/ProductionLineView.vue)
- 新增 /api/dashboard/production-line REST API

#### 2. 工位管理功能

- 新增 Workstation 实体类 (mes-common/entity/Workstation.java)
- 新增 WorkstationMapper 接口 (mes-common/mapper/WorkstationMapper.java)
- 新增工位管理前端页面 (mes-frontend/src/views/workstation/WorkstationView.vue)
- 新增 /api/dashboard/workstation REST API

#### 3. 基础数据控制器

- 新增 BaseDataController (mes-dashboard/controller/BaseDataController.java)
- 修复 @MapperScan 配置，添加 com.mes.common.mapper 扫描

#### 4. 权限管理增强

- 后端 RoleController 添加"生产线管理"和"工位管理"权限初始化
- 前端 permission.ts 添加对应菜单配置
- 前端 MainLayout.vue fallback 菜单添加生产线管理和工位管理

### Bug 修复

- 修复前端 form 初始化问题，添加默认值防止 undefined 问题
- 修复 DashboardApplication @MapperScan 只扫描 dashboard.mapper 的问题

---

## v1.0.26 (2026-05-04)

### 新增功能

#### 1. MES AI预测功能增强

根据MES AI功能开发提示词，实现以下功能：

##### 文件1: schemas/prediction.py
- QualityPredictionRequest - 质量预测请求模型
- QualityPredictionResponse - 质量预测响应模型
- BatchPredictionRequest - 批量预测请求模型
- BatchPredictionResponse - 批量预测响应模型
- ModelInfoResponse - 模型信息响应
- DeviceFaultPredictionRequest - 设备故障预测请求
- DeviceFaultPredictionResponse - 设备故障预测响应
- ProcessParamRecommendationRequest - 工艺参数推荐请求
- ProcessParamRecommendationResponse - 工艺参数推荐响应
- AnomalyDetectionRequest - 异常检测请求
- AnomalyDetectionResponse - 异常检测响应

##### 文件2: services/quality_predictor.py
- 特征工程: 数值归一化、编码、特征组合
- 模型加载: 加载ONNX或Pickle模型
- 推理: 返回预测结果和置信度
- 错误处理: 降级处理和缓存

##### 文件3: router/prediction.py
- POST /api/v1/predict/quality - 质量预测
- POST /api/v1/predict/batch - 批量预测
- GET /api/v1/predict/model/info - 模型信息
- POST /api/v1/predict/device/fault - 设备故障预测
- POST /api/v1/predict/process/recommend - 工艺参数推荐
- POST /api/v1/predict/anomaly - 异常检测

##### 文件4: models/train.py
- 数据加载: 支持CSV和模拟数据
- 特征工程: 特征组合、归一化
- LightGBM训练: 二分类模型
- 模型保存: PKL和ONNX格式

##### 文件5: tests/test_prediction.py
- 18个测试用例全部通过
- 请求/响应模型测试
- 端点测试
- 推理准确性测试

### 文件更新

1. mes-ai-service/README.md - 更新API文档
2. mes-ai-service/requirements.txt - 添加pytest、joblib、onnx依赖
3. mes-ai-service/Dockerfile - 添加models和tests目录

### 问题修复

#### 1. 逻辑删除问题修复
- 禁用@TableLogic，改用物理删除
- 修复删除后无法创建相同编码数据的问题
- 删除多余文档

#### 2. 菜单和权限管理
- 新增MenuView.vue菜单管理页面
- 新增PermissionView.vue权限管理页面
- 新增PermissionController.java权限管理接口
- 增强MenuController.java CRUD接口

#### 3. 数据库编码修复
- 修复JDBC连接字符编码为UTF-8
- 修复数据库中文乱码问题

#### 4. Kafka集成
- 添加Kafka Docker容器到docker-compose.yml
- 恢复mes-dashboard Kafka配置
- 实现设备数据Kafka消费

---

## v1.0.25 (2026-05-02)

### 新增功能

#### 1. WPF设备模拟器
- 创建WPF桌面应用替代旧的Node.js模拟器
- 支持设备创建/更新/删除操作
- 支持实时数据模拟推送
- 支持亮色/暗色主题切换
- 现代化UI设计

#### 2. 设备管理API
- POST /api/dashboard/device - 创建设备
- PUT /api/dashboard/device - 更新设备
- DELETE /api/dashboard/device/{deviceCode} - 删除设备
- DELETE /api/dashboard/devices/all - 清空所有设备

#### 3. 前端优化
- 设备列表每5秒自动刷新
- 搜索支持设备ID和名称
- 运行时长根据最后心跳真实计算
- 修复设备数据显示

#### 4. 代码清理
- 删除旧的mes-device-simulator (Node.js版本)
- 统一使用WPF设备模拟器

---

## v1.0.24 (2026-04-13)

### 新增功能

#### 1. 权限管理系统
- 创建角色管理模块 (RoleController.java)
- 创建菜单管理模块 (MenuController.java)
- 创建用户管理模块 (UserController.java)
- 创建权限管理前端页面 (RoleView.vue, UserView.vue)

#### 2. 数据库表结构
- sys_role - 角色表
- sys_permission - 权限表
- sys_menu - 菜单表
- sys_role_permission - 角色权限关联表
- 新增V4迁移脚本 (V4__permission_enhance.sql)

#### 3. 前端权限控制
- 创建权限store (permission.ts)
- 创建权限指令 (v-permission)
- 动态菜单加载
- 角色管理CRUD

#### 4. 登录认证修复
- 修复BCrypt密码验证问题
- 支持明文密码和BCrypt双模式验证

### 文件更新

1. 后端 (mes-auth)
   - AuthApplication.java - 添加Mapper扫描
   - RoleController.java - 角色CRUD + 权限分配
   - MenuController.java - 菜单查询
   - UserController.java - 用户管理
   - AuthService.java - 密码验证修复
   - application.yml - 添加mapper扫描配置

2. 公共模块 (mes-common)
   - Role.java - 添加sort字段
   - Menu.java - 添加children字段
   - Permission.java - 添加children字段
   - RolePermission.java - 新增实体
   - RoleMapper.java - 新增
   - PermissionMapper.java - 新增
   - MenuMapper.java - 新增
   - RolePermissionMapper.java - 新增

3. 前端 (mes-frontend)
   - router/index.ts - 添加role路由
   - main.ts - 添加权限指令
   - vite.config.ts - 添加auth代理
   - stores/permission.ts - 权限store
   - views/role/RoleView.vue - 角色管理页面
   - views/user/UserView.vue - 用户管理页面
   - views/layout/MainLayout.vue - 动态菜单
   - directives/permission.ts - 权限指令

4. SQL脚本
   - sql/init.sql - 更新用户密码
   - sql/V4__permission_enhance.sql - 权限增强迁移
   - sql/fix_password.sql - 密码修复脚本

---

## v1.0.23 (2026-04-08)

### 新增功能

#### 1. 个人中心和系统设置
- 创建个人中心页面 (ProfileView.vue)
- 创建系统设置页面 (SettingsView.vue)
- 添加真实数据库字段支持

#### 2. 真实用户数据集成
- 添加后端API: PUT /auth/profile
- 添加后端API: PUT /auth/password
- 更新User实体添加新字段

---

## v1.0.22 (2026-04-06)

### 新增功能

#### 1. UI美化优化
- 优化全局样式 (global.scss)
- 统一设计系统 - 按钮、卡片、输入框
- 优化MainLayout布局和样式
- 修复下拉框样式问题
- 添加主题切换功能

#### 2. 数据库更新
- V2迁移: 添加删除功能字段

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