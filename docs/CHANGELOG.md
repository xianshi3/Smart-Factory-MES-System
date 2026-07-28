# 更新日志 (Changelog)

## 项目概述

Smart Factory MES System - 智能工厂制造执行系统

- **创建日期**: 2026-04-04
- **技术栈**: Java (Spring Cloud) + .NET 8 + Python + Vue 3 + WPF
- **架构**: 微服务架构，支持2000+设备并发连接

---

## v1.0.36 (2026-07-28)

### AI 生产助理 — 对话历史 + MySQL 持久化

#### 对话历史功能

- **MySQL 持久化**: 对话记录从 SQLite 迁移到 MySQL `mes_db`，新增 `ai_chat_conversations` + `ai_chat_messages` 两张 InnoDB 表
- **用户关联**: 对话按 `user_id` 隔离，通过 Pinia `useUserStore` 获取登录用户名，每个用户只能看到自己的对话
- **历史管理**: 侧边栏展示对话列表（标题/时间/删除），支持新建、切换、删除对话
- **自动标题**: 首条用户消息自动截取前 30 字符作为对话标题
- **逻辑删除**: 对话采用 `deleted=1` 逻辑删除，数据可恢复

#### 前后端联动

| 层 | 改动 |
|---|---|
| Python 后端 | `conversation_store.py` 改用 PyMySQL 直连 MySQL；API 端点新增 `user_id` 查询参数 |
| Vue 前端 | 新增 `stores/aiChat.ts` Pinia store；`AiAssistant.vue` 全面改用 store 管理消息 |
| 数据库 | 新增 `V7__ai_chat_history.sql` 迁移文件；`init.sql` 同步建表 |

#### 企业级架构

```
mes-frontend → Pinia AiChatStore → Python mes-ai-service → PyMySQL → MySQL mes_db
                                              ↕
                                   mes-ai-service 独立拥有 ai_chat_* 表
                                   Java 微服务各自拥有业务表
                                   每服务独享数据，标准微服务模式
```

---

## v1.0.35 (2026-07-28)

### AI Agent 生产助理

#### Agent 架构

基于 GLM-4 function calling 的工业 Agent，支持自然语言→工具调用→任务闭环。

```
用户输入 → 意图理解 → 工具调用 → 结果处理 → 多步推理 → 最终回复
                          ↓
                    MES REST API (设备/工单/库存)
                          ↓
                    RAG 知识库 (设备手册/质检标准)
```

#### 后端 Agent 服务

- **Tool 定义层** (`src/services/tools.py`)：10 个 MES 工具，包括
  - `list_devices` / `get_device_detail` — 设备状态查询
  - `list_work_orders` / `create_work_order` — 工单管理
  - `list_production_lines` / `list_workstations` — 生产线/工位
  - `list_boms` / `list_materials` / `get_inventory` — BOM/物料/库存
  - `query_device_docs` — 知识库文档检索
- **Agent 编排** (`src/services/agent_service.py`)：基于 GLM-4 function calling，支持最多 8 步迭代推理
- **RAG 知识库** (`src/services/knowledge_base.py`)：轻量级关键词检索，内置 6 篇预设文档（CNC 操作手册、温度异常处理、质检标准、SPC 标准、工单规范、维护计划）
- **Agent API** (`src/router/agent.py`)：`POST /api/v1/agent/run` + 知识库搜索 + 工具列表查询

#### 前端 AI 助手

- **AiAssistant 组件**：浮动按钮触发，对话气泡展示，支持 Markdown 渲染
- **执行步骤可视化**：每步工具调用的参数和结果可折叠展开
- **快捷指令**：设备状态、设备诊断、创建工单、查手册一键发送

---

## v1.0.34 (2026-07-28)

### 企业标准化改造

#### 后端架构分层标准化

- **新增 Service 层**：为 ProductionLine 和 Workstation 创建独立 Service 接口和实现
  - `ProductionLineService` / `ProductionLineServiceImpl`
  - `WorkstationService` / `WorkstationServiceImpl`
- **消除 Controller 直调 Mapper**：`BaseDataController` 改为注入 Service 而非 Mapper
- **统一 CRUD 模式**：所有模块遵循 Controller → Service(接口) → ServiceImpl → Mapper 分层

#### 逻辑删除统一修复

- **移除全局 `logic-delete-field`**：4 个模块的 `application.yml` 全部清理
  - `mes-dashboard` / `mes-auth` / `mes-workorder` / `mes-quality`
- **统一使用物理删除**：`deleteById()` 执行 `DELETE` 而非 `UPDATE SET deleted=1`
- **所有 create 方法统一 `setDeleted(0)`**：ProductionLine / Workstation / Material / BOM
- **所有 list 方法统一过滤 `.eq(Deleted, 0)`**：避免返回已删除数据
- 修复逻辑删除导致的唯一约束冲突（create → delete → recreate 报 DuplicateKey）

#### DTO 与实体规范化

- `BaseEntity.deletedTime` / `deletedBy` 添加 `@TableField(exist = false)`，消除 MyBatis 映射异常
- `BomServiceImpl.createBom()` 添加 BOM 编号自动生成兜底（`BOM-YYYYMMDD-XXXX` 格式）

### 3D 数字孪生设备监控

#### 前端工程化

- **3D 场景组件**：新增 `DigitalTwinScene.vue` 基于 Three.js
  - 立式加工中心 CNC 模型（封闭式机柜、滑门视窗、控制面板、叠灯塔、刀库）
  - 动态厂房环境（地板/安全标线/立柱/桁架/围墙根据设备数量自适应缩放）
  - 实时动效：主轴旋转、输送带物料流动、LED 呼吸灯、故障脉冲环
  - 多点射线点击检测（±6px 容差）提升交互体验
  - 多视角预设切换（透视图/俯视图/前视图）
- **企业级控制室布局**：三栏式设计（KPI 胶囊 + 3D 场景 + HUD 面板）
- **BOM 管理页面**：新增 `BomView.vue` 物料清单页面

#### 架构优化

- **ESLint + Prettier**：新增 `.eslintrc.cjs`、lint/format 脚本
- **代码分割**：Vite 配置 manualChunks，index chunk 从 1810kB 降至 52kB
- **WebSocket**：设备数据毫秒级推送，实时映射 3D 场景
- **JWT 认证**：AuthController 重构为 HttpServletRequest 获取 Token
- **JwtAuthFilter**：新增认证过滤器

---

## v1.0.33 (2026-07-27)

### 新增功能

#### 数字孪生设备监控

- **3D 数字孪生场景**: 基于 Three.js 构建工厂车间三维可视化
  - 立式加工中心 CNC 模型（封闭式机柜、滑门视窗、控制面板、叠灯塔、刀库）
  - 动态厂房环境（地板/安全标线/立柱/桁架/围墙根据设备数量自适应缩放）
  - 实时动效：主轴旋转、输送带物料流动、LED 呼吸灯、故障脉冲环
  - 多点射线点击检测（±6px 容差）提升交互体验
  - 多视角预设切换（透视图/俯视图/前视图）
- **企业级控制室布局**: 三栏式设计
  - 顶部 KPI 胶囊状态栏（设备总数/运行/空闲/故障）
  - 主 3D 场景全屏展示
  - HUD 蒙层面板：告警列表（右上）、性能趋势图（左下）、设备详情（右下）
  - 视图切换：3D 孪生 / 列表卡片双模式
- **设备信息面板**: 温度颜色高亮（绿<55°C<橙<70°C<红）、六指标+利用率进度条、AI 预测/SPC/能耗快捷按钮

#### 前端工程化

- **ESLint + Prettier**: 新增 `.eslintrc.cjs`、lint/format 脚本
- **代码分割**: Vite 配置 manualChunks（vendor-vue/vendor-ui/vendor-chart/vendor-three），index chunk 从 1810kB 降至 52kB
- **菜单优化**: 默认全部展开 (`:default-openeds`)

### 修复

- **端口冲突修复**: AI 服务 8086→8087，消除与 InfluxDB 端口冲突
- **ONNX 模型缺失**: 产量预测添加 fallback 逻辑，不再因缺模型 crash
- **JWT 密钥**: 移除硬编码默认值，仅依赖环境变量 `JWT_SECRET`
- **MQTTnet 版本**: 4.3.6.992→4.3.6.1152，消除 NuGet 警告
- **docker-compose profile**: mes-process `dev`→`docker` 与其他服务一致
- **3D 视图分页**: 列表分页仅在 list 模式显示
- **相机自动旋转**: 3D 场景默认关闭自动旋转
- **设备名字段**: DigitalTwinScene 修正 dataLabel 字段映射 (`deviceName`→兼容 `name`)
- **按钮点击**: 告警/详情/图表面板互不覆盖，左右分区摆放
- **缺失闭合标签**: 修复 `dt-scene-wrap` 遗漏的 `</div>`
- **HUD 主题**: 所有面板使用 CSS 变量，兼容亮/暗主题切换
- **列表卡片**: 缩小最小宽度(280→220px)、增加 pageSize(12→50)，更多设备单页展示

### 优化

- **CNC 模型重设计**: 从简单方块升级为逼真立式加工中心（柜体/滑门/内部工作台+主轴/控制面板/叠灯塔/刀库/排屑抽屉）
- **天花板桁架**: 改为半透明细线（opacity 0.25），不再遮挡设备
- **安全标线动态化**: 随设备网格数量自动扩缩
- **Raycaster 优化**: 仅对 deviceNodes 做碰撞检测，排除厂房元素干扰

---

## v1.0.32 (2026-07-27)

### 新增功能

#### BOM + 物料管理

- **数据库**: 新增 5 张表 (material/bom/bom_item/inventory/inventory_transaction)
- **后端**: 5 个 Mapper + BomService + BomServiceImpl + 3 个 Controller (Material/Bom/Inventory)
  - 物料 CRUD + 搜索过滤
  - BOM CRUD + 嵌套明细管理 + 物料存在性校验 + 循环引用检测
  - 库存查询 + 库存调整 + 交易记录追溯
- **前端**: MaterialView.vue 物料列表/新建/编辑/删除
- **路由**: /material, /bom 加入路由和菜单

### 前端优化

- 移除所有视图的 animationDelay（消除页面卡顿）
- 删除 3 个死组件 (PageHeader/DataTable/SearchBar)
- CSS 去重 269 行，创建 useChartTheme composable
- 统一 8 个视图内边距，修复 16 处硬编码颜色
- 物料管理列表：极简企业风格、自适应列宽、库存预警、unitLabel 中英文映射

---

## v1.0.31 (2026-07-27)

### 修复

- **前端卡片不可见**: 3 个视图的 `.template-card`/`.order-card`/`.stat-item`/`.device-card` 使用 `animation: fadeInUp` 但未定义 `@keyframes fadeInUp`，`animation-fill-mode: forwards` 导致永久 `opacity: 0`
- **页面卡顿**: 移除所有视图的 `animationDelay` 内联样式（每张卡片依次动画导致卡顿）
- **Vite 代理 404**: `/process` 代理的 `rewrite` 错误剥离 `/process` 前缀导致 404
- **Element Plus 暗色主题**: 导入 `dark/css-vars.css` 使 ElTable/ElDialog 等组件支持主题切换
- **数据加载报错被吞**: `res.data.records` 加 `?.` 可选链防止 TypeError 被 `catch` 吞掉

### 优化

- 删除 3 个死组件 (PageHeader/DataTable/SearchBar)
- CSS 去重 269 行（7 个视图删除重复 page-header；stats-grid 合并到 global；status-tag 统一使用 CSS 变量）
- 创建 `useChartTheme` composable，3 个视图复用图表颜色
- 统一 8 个视图内边距为 MainLayout.el-main 的 24px
- 修复硬编码颜色 16 处，改用 CSS 变量
- 表状态标签改用 `var(--success-light)` 等变量替代硬编码 `rgba()`

---

## v1.0.30 (2026-07-27)

### 变更

- 移除 `start-all.bat` 和 `clean.bat`，改用 `Makefile`（跨平台兼容）
- 开始时间：`start-all.bat` → `make all`

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