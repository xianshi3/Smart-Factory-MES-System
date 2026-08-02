# Smart Factory MES System — 面试问答整理

---

## 一、项目概述

**Q: 简单介绍一下这个项目**

> 这是一个面向离散制造业的智能工厂 MES（制造执行系统），基于微服务架构设计，支持 2000+ 设备并发连接。覆盖了生产工单管理、工艺参数配置、质量检验追溯、设备实时监控、AI 智能预测、AI Agent 生产助理、生产报表统计等核心业务模块。

---

## 二、技术选型与架构

**Q: 技术栈是怎样的？为什么这么选？**

| 层级 | 选型 | 理由 |
|------|------|------|
| 前端 | Vue 3 + TypeScript + Element Plus | 响应式、TypeScript 类型安全、生态成熟 |
| 3D 可视化 | Three.js | 数字孪生场景渲染，设备状态实时映射 |
| 后端 | Spring Boot 3.2 + Spring Cloud 2023 | 微服务治理、服务注册发现、负载均衡 |
| ORM | MyBatis-Plus | 代码生成、Lambda 查询、分页插件 |
| AI Agent | GLM-4 + Function Calling + RAG | 自然语言驱动工业任务编排 |
| 数据库 | MySQL + Redis + InfluxDB | 关系数据 / 缓存 / 时序数据各司其职 |
| 消息队列 | Kafka | 高吞吐设备数据采集（10000+ 条/秒） |
| 设备接入 | .NET 8 + MQTT | 工业协议适配，MQTT 轻量级物联网协议 |
| AI 推理 | Python FastAPI + LightGBM | 质量预测、设备故障预警 |

**Q: 微服务怎么划分的？**

> 6 个核心服务：
> - **mes-gateway** (9090) — API 网关，统一路由、鉴权、限流
> - **mes-auth** (8081) — 认证服务，JWT + RBAC 权限控制
> - **mes-workorder** (8082) — 工单服务，工单全生命周期管理
> - **mes-process** (8083) — 工艺服务，工艺模板与参数配置
> - **mes-quality** (8084) — 质量服务，质检记录与追溯
> - **mes-dashboard** (8085) — 看板服务，实时数据聚合与 WebSocket 推送
>
> 外加独立部署的 .NET 设备网关 (5000)、Python AI 服务 (8087)、Vue 前端 (3000)。

---

## 三、核心功能与难点

**Q: 这个项目参加了什么比赛？**

> 参加了 **GOAI 世界人工智能开源大赛 — Boundless Agents 无界应用赛道**，赛题是"AI+工业制造"。
>
> 作品在现有 MES 系统上增加了 **AI Agent 生产助理**，实现了从自然语言到任务执行再到结果反馈的完整闭环。详细作品说明见 [`docs/COMPETITION.md`](./COMPETITION.md)。
>
> 核心亮点：
> - **不是泛聊天机器人**，而是可运行、可演示、可复用的工业 Agent
> - 支持多步推理编排（如"查设备→判断温度→创建维修工单"）
> - RAG 知识库增强（设备手册+质检标准文档检索）
> - 工具调用步骤可追溯、前端可视化展开
> - 基于真实 MES 系统构建，具备完整业务数据闭环

**Q: 比赛文档有哪几个核心部分？**

> 我们准备了专门的 [`COMPETITION.md`](./COMPETITION.md)，覆盖了大赛要求的所有维度：
>
> | 要求 | 对应内容 |
> |------|----------|
> | 目标用户与场景痛点 | 生产班长、维护工程师、物料管理员、车间主任 |
> | 交互流程 | 3 个完整 Demo 演示（设备查询、知识检索、多步综合任务） |
> | 技术路线 | 架构图 + Agent 编排流程图 + 工具接口表 |
> | 数据来源与合规 | 全部为模拟数据，本地部署，JWT 认证 |
> | 可运行 Demo | 完整前后端可本地部署运行 |
> | 迭代计划 | 短期/中期/长期技术演进路线 |
> | 与评分标准对应 | 场景真实度/闭环完整性/技术复杂度/可演示性/可复用性 |

**Q: 说说你负责的核心功能**

> 主要负责 **mes-dashboard 看板服务**、**mes-ai-service Agent 服务** 和 **mes-common 公共模块**，涉及：
>
> 1. **生产线/工位管理** — CRUD + Service 层标准化
> 2. **BOM 物料清单** — 嵌套明细管理、物料校验、库存联动
> 3. **设备实时监控** — Kafka 消费、WebSocket 推送、3D 数字孪生
> 4. **AI Agent 生产助理** — 工具定义、Agent 编排、RAG 知识库
> 5. **公共基础设施** — 统一异常处理、统一返回格式、实体基类

**Q: 遇到过哪些技术难点？怎么解决的？**

---

### 难点 1：逻辑删除 + 唯一约束冲突

**现象：** 创建 → 删除 → 再次创建相同编码的数据，报唯一约束冲突。

**排查过程：**

1. 先查数据库，发现删除操作实际上是 `UPDATE SET deleted=1`（逻辑删除），数据还在
2. 检查发现 `BaseEntity` 的 `@TableLogic` 已注释，但 `application.yml` 中 `logic-delete-field: deleted` 全局配置仍生效，二者打架
3. 部分表（`mes_workstation`、`mes_production_line`）的唯一索引只有 `(code)`，不包含 `deleted`，导致 `deleted=1` 的记录与新插入的 `deleted=0` 的记录冲突

**解决方案：**

- 移除了所有模块 `application.yml` 中的 `logic-delete-field`，统一使用物理删除
- 为所有 create 方法加上 `setDeleted(0)`，所有 list 查询加上 `.eq(Deleted, 0)` 过滤
- 补齐 Service 层，消除 Controller 直调 Mapper 的不规范写法
- 统一 CRUD 模式，避免后续 AI 生成代码继续踩坑

---

### 难点 2：BOM 管理中 version 字段与实体类冲突

**现象：** BOM 列表查询返回空数据，数据实际存在。

**排查过程：**

- `Bom` 实体类继承 `BaseEntity`，`BaseEntity` 有 `getVersion()` 方法
- MyBatis-Plus 在映射时混淆了 `Bom.version`（BOM 版本号）和 `BaseEntity` 的 `version` 字段
- Lambda 查询条件 `Bom::getVersion` 指向了 BOM 版本号，但 MyBatis-Plus 解析时优先匹配基类

**解决方案：**

- `Bom` 实体中显式重命名 getter 为 `getBomVersion()`，配合 `@JsonProperty` 保持前端兼容

---

### 难点 3：3D 数字孪生性能优化

**现象：** 50+ 设备同时渲染时页面卡顿，Three.js 场景加载慢。

**解决方案：**

- Vite 代码分割：vendor-vue / vendor-ui / vendor-chart / vendor-three 分离，index chunk 从 1810kB 降至 52kB
- Raycaster 优化：只对设备节点做碰撞检测，排除厂房元素
- 锁定帧率，非激活标签页暂停渲染
- HUD 面板与 3D 场景分离渲染，互不阻塞

---

### 难点 4：AI Agent 工具调用编排

**现象：** 用户一句话需要拆成多个步骤，且后一步依赖前一步结果。

**方案：** 基于 GLM-4 function calling 实现多步推理：

```
用户: "查 DEV-001 状态，温度高就创建维修工单"
Step 1: get_device_detail("DEV-001") → 温度 68°C
Step 2: 判断 68 > 55 → 需要创建工单
Step 3: create_work_order("DEV-001 高温维修", 1, "HIGH")
Step 4: 汇总结果回复用户
```

- 使用 GLM-4 的 `tools` 参数定义工具 Schema
- 每次返回 `tool_calls` 时自动调用对应函数，结果注入下一轮对话
- 设置 `max_iterations=8` 防止死循环

---

## 四、架构决策

**Q: 为什么从逻辑删除改成物理删除？**

> 因为这个项目没有"回收站"业务需求——数据删除就是真的删除，不需要恢复。逻辑删除反而带来了：
> 1. 唯一约束冲突（create → delete → recreate）
> 2. 所有查询都要手动加 `deleted=0`
> 3. 数据表越积越大

**Q: Agent 为什么选择 GLM-4 而不是 LangChain？**

> 1. 团队已有 ZhipuAI 的 API Key 和集成经验
> 2. GLM-4 原生支持 function calling，不需要额外框架
> 3. LangChain 引入大量依赖，对比赛作品来说过于重
> 4. 自定义编排逻辑更可控，方便调试和展示每一步执行过程

**Q: Docker 只跑基础设施，Java 服务跑在宿主机，为什么这么设计？**

> 调试方便。Java 服务在 IDE 里直接启动可以热部署、断点调试。Docker 只用于 MySQL、Redis、Kafka、Nacos 这些基础设施，统一版本、一键拉起。

---

## 五、团队协作与代码质量

**Q: 怎么保证团队代码风格统一？**

> 通过 **企业标准化**：
> 1. 统一实体基类 `BaseEntity`（ID、时间、删除标记）
> 2. 统一返回格式 `Result<T>` + 全局异常处理 `GlobalExceptionHandler`
> 3. 统一分层：Controller → Service(接口) → ServiceImpl → Mapper
> 4. 统一 CRUD 模板：create 初始化、list 过滤、物理删除
> 5. 在项目根目录维护约束文档，AI 工具每次生成代码会自动遵守规则

---

## 六、常见追问

**Q: Agent 的 RAG 知识库怎么实现的？**

> 轻量级方案——使用关键词检索（Jaccard 相似度）匹配文档，原因：
> - 知识库规模小（6 篇文档），不需要向量数据库
> - 零外部依赖，pip install 即用
> - 可扩展：预留了 `knowledge/` 目录，后续可以接入 ChromaDB
> 内置文档包括 CNC 操作手册、温度异常处理、质检标准、SPC、工单规范、维护计划

**Q: Agent 的 Tool 有哪些？**

> 10 个工具：设备列表、设备详情、工单列表、创建工单、生产线列表、工位列表、BOM 列表、物料列表、库存查询、知识库检索。每个工具对应一个 MES 后端 REST API，通过 httpx 异步调用。

**Q: 项目有多少代码量？**

> Java 后端约 3 万行，前端 Vue/TS 约 2 万行，Python AI 服务约 4000 行（含 Agent 层），.NET 设备网关约 2000 行。

---

## 七、一句话总结

> 这是一个基于 Spring Cloud 微服务架构、覆盖制造执行全流程、集成 3D 数字孪生、AI 预测和 Agent 智能助手的工业互联网平台，参与了 GOAI 世界人工智能开源大赛，验证了 AI Agent 在工业场景中的落地可行性。
