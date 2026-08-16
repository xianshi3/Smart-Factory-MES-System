# 更新日志 (Changelog)

## 项目概述

Smart Factory MES System - 智能工厂制造执行系统

- **创建日期**: 2026-04-04
- **技术栈**: Java (Spring Cloud) + .NET 8 + Python + Vue 3 + WPF
- **架构**: 微服务架构，支持2000+设备并发连接

---

## v1.0.51 (2026-08-16)

### 工作台全面升级

- **一屏网格布局**：欢迎栏单行（标题+时间同行）、系统状态单行条、左列（统计 2×2 + AI 洞察）、右列 3D 孪生、底部设备横排迷你卡，实测无需滚动
- **三合一 3D 工厂总览**（FactoryTwin）：3D 场景 + 底部生产趋势/状态分布图表卡片行；厂房线框、设备阵列状态着色、告警呼吸灯、自动旋转、点击设备跳转
- **AI 生产智能洞察**：单次 LLM 生成 3 条洞察，条目卡片化（主题图标自动匹配：健康=红/产能=紫/告警=橙），打字机输出，底部元信息栏（模型/时间/耗时）+ 智能快捷追问（基于实时数据动态生成）
- **系统健康监控**：7 个服务 15s 轮询健康检查（延迟/在线统计），服务能连通即在线
- 数字滚动动画统计卡、毛玻璃数据浮层

### 报警中心升级

- **真实数据链路**：设备状态变化自动产生/解决告警（进入 ALARM 自动创建按温度分级，恢复后 SYSTEM 自动解决）；启动兜底扫描补齐存量告警设备
- 列表美化：级别胶囊/状态胶囊（活跃红点脉冲）/行色条/相对时间/多列排序
- 新功能：级别分布环形图、今日告警时段柱状图、批量确认/解决、导出 CSV、详情抽屉 + AI 告警分析（根因/处置建议）
- 修复：vite 代理缺 `/alarm` 路由、详情字段名错位、llm_service 上下文注入只认固定 key（alarm/通用键兜底）

### 登录页重设计（企业级）

- 移除自助注册/忘记密码（账号管理员统一开通）
- canvas 图形验证码（干扰线+噪点+点击刷新）、企业公告轮播（6s）、系统状态实时检测（15s 轮询）、演示账号快捷填充、记住上次账号
- 左侧品牌区：真实 logo + 渐变光晕 + 网格 + 浮动粒子 + 4 特性卡片，主题兼容

### 侧边栏与体验

- 侧边栏：渐变品牌区/选中指示条/hover 动效/底部版本信息，支持 `public/logo.png` 自定义 logo（失败回退内置图标）
- AI 助手接入 7 个页面（工单/排产/工艺/质量/报表/设备/报警），页面级会话隔离（autoNew）
- Agent 新增 4 个工具（工艺模板/质检记录/排产看板/生产报表）；LLM 上下文注入通用兜底
- Agent 超时保护（执行 100s/报告 30s/前端 150s）、vite 代理防崩溃（ECONNRESET）、5 个业务服务补 actuator 依赖

---

## v1.0.50 (2026-08-14)

### 开源规范完善

#### 安全性

- **依赖漏洞清零**：`npm audit` 修复 5 个漏洞（brace-expansion/js-yaml/nanoid 高危传递依赖 + echarts 5.6 中危）；echarts 升级 `5.6.0 → 6.1.0`、vue-echarts 升级 `7.0.3 → 8.1.0`（type-check/build/测试全部通过）；happy-dom 升级至 20.x 修复 CVE-2026（VM 逃逸）
- **CodeQL 静态安全扫描**：新增 `.github/workflows/codeql.yml`（Java/JS/Python 三语言，push/PR/每周定时）
- **Dependabot 依赖自动更新**：新增 `.github/dependabot.yml`（npm 每月 / Maven 每月 / GitHub Actions 每周，echarts 大版本人工验证）

#### 测试

- **后端单元测试**：mes-common 引入 `spring-boot-starter-test`，新增 `JwtUtilsTest`（密钥校验/签发解析/防篡改/跨密钥拒绝）与 `ResultTest`（统一返回/分页），共 10 用例
- **前端单元测试**：引入 Vitest + happy-dom，新增 `markdown.test.ts`（XSS 转义/标题层级/列表/代码）与 `auth.test.ts`（记住我 token 存储），共 10 用例
- **CI 集成**：前端 job 新增 `npm test` 步骤；后端 `mvn package` 自动执行单测
- **顺带修复真实 bug**：`mdToHtml` 有序列表被渲染成无序（数字被消费后无法区分 ol/ul），改用 `data-ord` 标记识别

#### 合规与文档

- **LICENSE 署名**：补充维护者 `xianshi3` 与 contributors
- **CONTRIBUTING**：提交前检查加入 `mvn test` / `npm test` / `pytest`，并注明鉴权改动需同步检查清单
- `.gitignore` 补充 `coverage/` 等测试产物

#### 待办（需仓库所有者操作）

- 发布版本：git tag 仅到 `v1.0.41`，CHANGELOG 已至 v1.0.50，建议按 CHANGELOG 打对应 tag 并创建 GitHub Release
- 启用 GitHub Security 功能：Settings → Code security → 开启 Dependabot alerts / CodeQL scanning / Secret scanning

---

## v1.0.49 (2026-08-14)

### 剩余中低危问题整改（异常处理 / 并发 / 校验 / 前端安全）

#### 后端

- **错误信息不再泄露内部细节**：`GlobalExceptionHandler` 500 不再返回底层 cause 消息（仅记录日志）；5 个服务 `server.error.include-message` 改为 `never`
- **控制器去掉吞异常 + 透传 `e.getMessage()`**：`PlanningBoardController` 全部接口移除 try/catch，统一交给全局处理器（业务冲突 `IllegalStateException` 仍 400 透传消息，其余 500 通用消息）；`WorkOrderController.create` 同上，`detail` 不存在返回 404
- **业务异常规范化**：process/quality/dashboard 的 `RuntimeException`（模板不存在/已发布、质检记录不存在、BOM/库存/设备不存在、库存不足等）全部改为 `BizException` → 返回 400 而非 500
- **库存调整并发保护**：`adjustInventory` 改为条件更新（`quantity >= |delta|` 才成功），并发调整不再互相覆盖，冲突时提示刷新重试
- **Kafka 消费竞态丢消息**：`findOrCreateDevice` 捕获唯一约束冲突后重查重试，不再吞掉并发首条消息；移除 debug 级完整消息日志
- **工艺模板死缓存清理**：移除只写不读且不失效的 `mes:process:template:{id}` 缓存写入
- **权限缓存即时失效**：`PermissionService.evictAll()` 在角色权限分配/权限码增删后调用，`evict(userId)` 在用户角色变更后调用（原来最长 5 分钟延迟）
- **参数校验补齐**：`CreateWorkOrderDTO`/`SubmitReportDTO`/`CreateTemplateDTO`/`CreateQualityRecordDTO` 增加 `@NotBlank/@Size/@Min/@Max/@Pattern`，控制器补 `@Valid`；三个分页接口 `size` 限制 ≤ 100
- **SQL 参数化**：报工数量累加 `setSql` 改用 `{0}` 占位符，杜绝拼接风险
- **OEE 防护**：`deviceId` 为空返回空数据（避免全表 selectOne 多行异常）；`idealCycleTime`/`totalProducts` 为空不再 NPE/除零
- **InfluxDB 行协议转义**：status 字段值转义引号/反斜杠/换行，防止破坏 line protocol
- **WebSocket 资源释放**：`DashboardWebSocketHandler` 增加 `@PreDestroy` 关闭广播调度线程池
- **健康检查白名单**：`TokenAuthFilter` 白名单增加 `/actuator/`（健康探针无需 token），支持目录前缀匹配

#### 前端

- **3D 标签 XSS 修复**：`DigitalTwinScene.vue` 设备名/状态等外部数据统一 `escHtml` 转义后再拼 innerHTML（此前可通过设备名注入脚本）
- **权限加载失败重试**：`permissionStore.loadCurrentPermissions` 首次失败不再置 `loaded=true`，下次路由跳转自动重试
- **Markdown 标题层级**：`####` 渲染为 h5、`#####` 渲染为 h6（原来三级/四级都输出 h4）

---

## v1.0.48 (2026-08-14)

### 安全加固 + 生产可用性修复（代码审查整改）

#### 高危安全

- **JWT 密钥强制环境变量化**：`JwtUtils` / 网关过滤器移除硬编码默认密钥，`JWT_SECRET` 为空或长度 < 32 时启动失败；所有服务 application.yml 默认值置空，compose/CI/脚本注入开发密钥
- **网关零认证修复**：新增 `JwtAuthGlobalFilter`（WebFlux），除登录/注册/健康检查外全部请求必须携带有效 Bearer Token，并向服务透传 X-User-Id 等头
- **CORS 收紧**：网关 `allowedOriginPatterns: "*" + allowCredentials: true` 改为白名单（默认 localhost:3000/5173，可用 `CORS_ALLOWED_ORIGINS` 覆盖）
- **权限注解补全**：process/quality/dashboard/menu/alarm/bom/material/inventory/base-data 控制器补齐 `@RequirePermission` / `@RequireRole`，堵住"登录即全通"越权（删除接口不再信任可伪造的 `X-User-Id` 请求头，改用 `UserContext`）
- **密码明文入库修复**：`UserController.create/update` 统一 BCrypt 加密；`AuthService` 移除明文比对兜底分支
- **InfluxDB Token / 智谱 API Key 清理**：dashboard 默认 token 置空（未配置时优雅降级），docker-compose.dev.yml 改用新随机 token + 环境变量；工作区 `.env.local` 中真实 API Key 已清除
- **WebSocket 鉴权**：握手拦截器校验 `?token=` JWT，未授权连接被拒绝；新增网关 `/api/ws/**` WebSocket 路由
- **AI 服务鉴权**：新增 `src/security.py`（HS256 JWT 校验，标准库实现），全部业务接口（除 /health）要求有效 JWT；Agent 工具调用后端时透传用户 JWT

#### 功能 Bug

- **生产环境接口双 `/api` 前缀**：前端所有 URL 统一去掉 `/api` 前缀（`VITE_API_BASE_URL=/api` 与网关 StripPrefix 配合），BOM/报警/生产线/工位等接口恢复可用；`agent.ts` 复用统一 axios 拦截器（带 token/401 处理）
- **库存调整 500**：`Inventory.availableQuantity` 为数据库 STORED 生成列，标记 `@TableField(exist=false)` 不再写入
- **SQL 迁移脚本幂等化**：V2/V4/V5_5/V9/V10/V11 改为存储过程条件判断（列/索引存在即跳过）；V5_5 修正错误列名（`param_name`/`report_time`）；V7/V8 改 `CREATE TABLE IF NOT EXISTS`（不再 DROP 丢数据）；V9 移除"全库重置密码为 admin123"；V11 工序种子按 (template_id, step_no) 去重；V6 库存种子加 `batch_no='INIT-BATCH'` 使唯一键可去重
- **排产撤销栈跨用户污染**：`PlanningBoardServiceImpl` 撤销栈改为按用户隔离（ConcurrentHashMap）
- **RoleController**：`assignPermissions` 不再吞异常（事务整体回滚）；`list` 移除 GET 写库与描述字段覆盖
- **aiChat 恒真过滤**：多轮对话历史只携带已持久化消息，不再重复发送当前消息

#### 中低危

- **WebSocket 前端**：单例引用计数（所有页面退订后才断开）、默认地址改为同源网关 WS + 自动带 token、`rememberMe` 勾选生效（sessionStorage/localStorage）
- **docker-compose**：MySQL/Redis/Zookeeper/Kafka/EMQX/InfluxDB 增加 healthcheck 与 `depends_on` 条件；Kafka 增加持久化卷；网关端口统一 `9090:9090`；移除未使用的 Nacos/Seata 服务
- **数据库密码统一 `123455`**（compose/application.yml/CI/文档），可通过 `MYSQL_ROOT_PASSWORD` 环境变量覆盖
- **CI**：`Lint Frontend` 步骤实际执行 `npm run lint` 且不再 `continue-on-error`；补齐 type-check 独立步骤；冒烟脚本注入 `JWT_SECRET`
- **docker-daemon.json**：替换 4 个已停服镜像源
- **Makefile stop**：改为按端口杀进程（不再依赖窗口标题）
- **MenuController**：按当前用户权限码过滤菜单，非 ADMIN 不再看到全部管理菜单
- **趋势接口**：`days` 限制 1~366，防止 N+1 循环查询

---

## v1.0.47 (2026-08-14)

### 权限体系重构 + 修复权限码缺失导致功能被隐藏

#### 后端（mes-common）

- **AOP 切面 → Spring MVC 拦截器**：`SecurityAspect`（依赖 aspectjweaver）替换为 `SecurityInterceptor`（`HandlerInterceptor`）+ `SecurityWebConfig` 自动注册，功能完全一致（`@RequireRole` / `@RequirePermission` 类级/方法级、401/403），**零 AspectJ 依赖**——修复运行环境缺 `aspectjweaver` 导致全部服务启动失败的问题
- 移除 `spring-boot-starter-aop` / `aspectjweaver` 依赖
- 修复 mes-process / mes-quality 的 `@MapperScan` 缺少 `com.mes.common.mapper`（PermissionService 依赖的 `SysUserAuthMapper` 无法注入）

#### 数据库（sql/V13）

- 新增 `V13__sync_permission_codes.sql`（幂等）：补齐 `sys_permission` 缺失的 19 条权限码（`workorder:view/create/edit/delete`、`process:view`、`quality:view`、`device:view`、`report:view`、`role:manage`、`permission:manage` 等），并为 ADMIN/MANAGER/USER/QC/ENGINEER 重新分配角色权限
- 清理一次性修复脚本：删除 `fix_charset.sql` / `fix_charset2.sql` / `fix_password.sql` / `add_alarm_menu.sql`（内容均已并入 init.sql / V5 / V6）

#### 前端（mes-frontend）

- `hasPermission` 对 ADMIN 角色全量放行，与后端 `PermissionService` 语义对齐——修复 `sys_permission` 数据不完整时 admin 菜单被按权限码过滤隐藏的问题

#### CI（GitHub Actions）

- 新增 `smoke` job：MySQL + Redis 就绪后**真实启动** auth/workorder/process/quality/gateway 五个服务，逐个等待就绪，再经网关验证「登录 → 带 token 访问工作台(200) → 无 token 被拦截(401)」完整链路，防止「编译通过但运行期挂掉」类回归
- `setup-java` 升级 v5

---

## v1.0.46 (2026-08-13)

### 生产调度看板（APS 排产）全量上线

新增企业级工序级排产看板 `Production Scheduling Board`（/planning-board），实现「拖拽排产 + 自动排程 + 冲突检测 + 冻结下发 + 变更追溯」完整闭环。

#### 数据库（sql/V10 + V11）

- **V10 排产基础表**: `wo_schedule`（排产明细，工序级，含 planned_start/end、status PLANNED/FROZEN/RELEASED、sort_order、operator_id）、`wo_schedule_log`（变更日志，含 action/action_desc/operator_id/create_time）、`mes_shift`（班次表，DAY 08-18 / NIGHT 20-06）、`mes_work_calendar`（工作日历 60 天）
- **V11 工艺工序与产能**: 新增 `proc_step` 工序表（17 条种子工序），`mes_workstation` 增加 `capacity_per_hour`（每小时产能）与 `is_bottleneck`（是否瓶颈）列，`work_orders` 增加 `sort_order`（排产顺序），工单关联工艺模板
- V10.1 演示数据：为加工类工单补计划时间并展示排产各状态

#### 后端（mes-workorder + mes-common）

- **实体与 Mapper**: `ScheduleItem`/`ScheduleLog`/`Shift`/`WorkCalendarDay`/`ProcStep` 实体 + 5 个 Mapper
- **APS 核心服务** `PlanningBoardServiceImpl`:
  - 看板聚合：设备分组（含负载率/瓶颈标记）、未排产工单池、班次、工作日历、时间冲突、变更日志
  - `auto-plan` 自动排程：按优先级+交期排序，工艺模板拆分工序，`pickLowestLoadWs` 负载均衡，工作日历+班次约束找空档，瓶颈工序标记
  - `move` 拖拽调整：单工序/整单移动、跨设备切换、时间吸附（30min）、`checkConflict` 冲突检测（重叠区间计算）、`force` 强制保存
  - `freeze/unfreeze/release` 冻结/解冻/下发：支持按工单/工序/设备范围；冻结与已下发工序禁止任何移动（含 force）
  - `undo` 撤销栈（内存 50 条，线程安全），`unassign` 取消排产回池
  - 日志审计：所有操作写入 `wo_schedule_log`，时间用应用本地时间（修复 MySQL UTC 8 小时时差）
- **雪花 ID 精度修复**: 排产 VO 全部 Long ID 字段 `@JsonSerialize(ToStringSerializer)` 序列化为字符串，解决 19 位 ID 超 JS Number 安全整数导致拖拽"排产明细不存在"的根因问题
- **异常处理**: `GlobalExceptionHandler` 新增 `IllegalStateException` 处理器，业务冲突/冻结拦截消息透传（400 而非 500 系统内部错误）
- `WorkOrderApplication` 补扫 `com.mes.common.mapper`

#### 前端（PlanningBoardView.vue 全新 + 主题化）

- **甘特图看板**: 设备泳道行（按工单分组工序条）、双级时间刻度（月份 + 日/小时）、默认 28 天窗口、横向滚动 + 纵向滚动（表头/设备标签 sticky 固定）
- **缩放控制**: 操作栏 −/+/重置 按钮，每日像素宽度 42px~300px 可调
- **拖拽交互**: 工序条拖拽调时间/跨设备（鼠标锚点跟随 + 30min 吸附）、左右手柄拉伸工时、待排产卡片拖入设备、拖拽落点幽灵（绝对定位跟随目标行）、Esc 取消；位移 <5px 视为点击不触发拖拽
- **选中与详情**: 点击工序条/泳道头选中（任意状态可选中，含冻结），右侧详情面板（10+ 字段、同工单工序列表、调整时间/整单移动/下发/取消排产操作）
- **右键菜单**: 调整时间、冻结/解冻、下发、取消排产、设备级冻结
- **自动排程**: 仅未排/全部重排（重排保护冻结与已下发工单不拆散），撤销按钮
- **乐观更新**: 拖拽成功本地移动不整页刷新，失败回滚并提示后端具体原因（冲突/不存在等）
- **主题兼容**: 全部改用全局 CSS 变量（暗色/亮色自适应），简约扁平风格（无发光），状态色/渐变/圆角与全局一致；工位标签三行分层布局（名称+状态点 / 编码+瓶颈+负载 / 细负载条）
- **细节**: 30s 轮询（拖拽中不刷新）、刷新后保持选中、`user-select: none` + `dragstart` 拦截（消除拖拽复制光标）、API 拦截器保留 `response.data` 供业务层读取后端消息

#### 其他

- 移除未使用的 `vue-draggable-plus` 依赖
- 工艺/质量视图清理重复的 `.status-tag` 样式定义与入场动画
- `.gitignore` 忽略本地 `var/` 运行产物

---

## v1.0.45 (2026-08-11)

### 四大业务模块企业级完整化（工单 / 工艺 / 质量 / 生产报表）

#### 工单管理（mes-workorder + WorkOrderView.vue）

- **工单关闭闭环**: 新增 `POST /workorder/{id}/close`，非 CLOSED/PENDING_QC 状态可关闭；关闭后不可再报工
- **编辑能力扩展**: `UpdateWorkOrderDTO` 新增产品名称/型号、计划数量、工位、工艺模板、计划开始/结束时间字段，仅 CREATED 状态可编辑业务信息
- **前端重写**: 卡片点击开详情弹窗（10+ 字段含工位/模板名称解析）、编辑模式、动态下拉（工位列表 + 工艺模板分页查询）、关闭按钮、筛选支持 PENDING_QC、报工校验（良品+不良=报工量）、清理调试日志

#### 工艺管理（mes-process + ProcessView.vue）

- **工序步骤管理**: 新增 `proc_step` 表 + `ProcessStep` 实体/`ProcessStepMapper`；模板详情 VO（`TemplateDetailVO` = 模板 + 参数 + 工序）聚合查询
- **模板复制**: `POST /template/{id}/copy` 一键复制为草稿副本，级联复制参数与工序步骤，编码自动加 "-副本"
- **参数/工序 CRUD**: 全套 `GET/POST/PUT/DELETE` 端点（按模板/按参数ID/按步骤ID），发布（PUBLISHED）后禁止修改，`assertTemplateEditable` 统一拦截
- **前端重写**: 详情弹窗（参数表格 + 工序步骤表格，草稿可增删改）、参数校验弹窗（逐参数输入值 → 通过/失败明细）、复制按钮、状态筛选（草稿/已发布）挂载查询

#### 质量管理（mes-quality + QualityView.vue）

- **追溯契约修复**: `forwardTrace` 返回体重写为 `TraceDetailVO`（sn/工单ID/工单号/质检结果/时间 + `steps[]` 工序链路），新增 `TraceStepVO`（工序/物料批次/设备/操作员/参数快照/时间）；修复此前返回数组而前端按单对象渲染导致的追溯永远空数据
- **不合格闭环**: 新增 PENDING（待检）状态流程 —— 新建待检记录 → 质检员执行「合格」/「不合格」（不合格原因必填，`POST /record/{id}/fail` 前端接线）
- **前端升级**: 卡片操作区分「追溯 / 合格 / 不合格 / 删除」，追溯弹窗时间线展示完整工序链路（操作员/物料批次/设备/参数快照）

#### 生产报表（mes-dashboard + ReportView.vue）

- **OEE 显示修复**: `dash_production_stats` 各率字段存 0-1 小数，旧实现直接输出导致前端显示 `0.9%`；现统一转换为百分数
- **OEE 分解**: 报表新增可用率 A / 性能率 P / 质量率 Q 逐日与平均值，前端新增 OEE 分解卡片（三色进度条）与明细表 A/P/Q 列
- **统计维度**: `GET /report/production` 新增 `dimension` 参数（day 按日 / workstation 按工位 / workOrder 按工单），按日统计自动补全无数据日期
- **趋势可视化**: 产量/良品柱状图叠加 OEE 与良品率双折线（双 Y 轴 + 图例）
- **明细分页**: 表格本地分页（10/20/50），CSV 导出补齐维度/OEE 分解列

### 其他修复

- **JWT 过期 401**: `GlobalExceptionHandler` 新增 `ExpiredJwtException`/`JwtException` 处理，过期返回 401「登录已过期」而非 500（需重启各服务生效）
- **AI 服务代理修复**: `tools.py` 设置 `trust_env=False`，消除系统代理（Clash）导致访问 localhost 后端 502 的问题
- **前端会话竞态修复**: 路由守卫 `beforeEach` 内 `await getUserInfo()` 后再渲染，修复 AI 聊天记录因 userId 竞态写入 `default` 导致历史丢失
- **init.sql 补齐**: 新增 `proc_step` 表定义

---

## v1.0.44 (2026-08-10)

### AI 生产助理智能体能力全面完善（mes-ai-service + mes-frontend）

- **任务理解**: 新增 `task_planner.py` — 规则+实体抽取的意图识别（设备监控/健康总览/告警诊断/工单/知识/分析/库存/闲聊 8 类），自动提取设备编码等实体并生成分步执行计划；强信号关键词优先判定，解决"温度过高怎么处理"误判为设备监控的问题
- **流程编排**: `agent_service.py` 重构为四阶段流水线（任务理解 → 计划执行 → 知识增强 → 结果交付）；执行计划注入 system 上下文指导工具调用；非可重复工具去重，防止 LLM 重复调用同一工具
- **工具调用**: `tools.py` 新增统一执行器 `execute_tool`（超时保护 + 错误规范化 + 绝不向上抛异常）；15 个工具全部改为容错请求（后端宕机返回结构化错误而非 500）；新增 `get_work_order_detail` 工单详情工具；新增 TOOL_META 元数据（分类/知识兜底标记）
- **知识增强**: `knowledge_base.py` 检索升级 — Jaccard → TF-IDF（中文双字词加权 + 标题命中重排），内置 6 篇种子文档 + `knowledge/` 目录自定义文档按 id 去重合并；新增 `retrieve_context()` 上下文注入，知识类任务预注入、设备诊断工具失败自动知识库兜底
- **多轮交互**: 会话焦点记忆（Redis `agent:focus:{session_id}`，TTL 30 分钟），"那台设备现在温度多少"自动继承上轮设备编码
- **结果交付**: 新增 `report_builder.py` — LLM 生成结构化交付报告（summary/关键结论/数据表格/处置建议/后续追问），JSON 解析失败自动规则兜底（从执行步骤提取关键信息）
- **API 扩展**: `/agent/run` 响应新增 `plan`（执行计划）、`report`（结构化交付）、`intent`/`intent_label`（意图）字段，向后兼容
- **前端升级**: AI 助手消息新增"执行计划"展示（序号/工具/目的）与"交付报告"卡片（摘要/关键结论/数据表格/处置建议）

### 其他

- 修复 AI 会话保存 500（`lastrowid` 在 UPDATE 后被 PyMySQL 重置为 0，改为 INSERT 后立即读取）

---

## v1.0.43 (2026-08-10)

### 设备模拟器全面升级（mes-device-simulator-wpf）

- **SCADA 上位机 UI 重设计**: 深色工业控制台风格（深蓝底 #0B1120 + 数据用 Consolas 等宽字体），双通道状态指示（API/MQTT 分离显示）、设备实时列表、参数区四色滑块（温度橙/转速青/压力紫/功率绿）
- **批量模拟**: 一键批量创建 N 台设备（`POST /api/dashboard/device/batch`），多台设备同时动态运行；修复批量模拟只有首台设备动态的 Bug（HTTP 推送原先只发首台，改为逐台推送全部设备）
- **场景预设**: 5 种生产场景一键切换（正常运行 / 满载生产 / 高温告警 / 突发故障 / 维护停机），各场景联动参数曲线与设备状态
- **实时曲线**: 内置 120 点滚动曲线面板，温度/转速实时绘制（不再依赖外部图表库）
- **频率控制**: 数据推送间隔可调（0.5s ~ 5s）
- **配置持久化**: API/MQTT 地址、设备参数、推送频率、场景选择自动保存至 `%APPDATA%/MESDeviceSimulator/config.json`，重启自动恢复
- **设备列表复选框选控**: 每台设备可勾选"参与模拟"，模拟/推送/曲线/场景仅作用于勾选设备，未勾选时自动退化为单台模式；连接 API 后自动加载后端已有设备到本地列表；新增"清空"按钮批量下线设备
- **自动探测**: 启动时自动探测 API 网关与 EMQX 地址（发现失败回退默认 localhost）
- **主题切换**: 亮色/暗色双主题，默认亮色；通过 DynamicResource 重构修复主题切换颜色错乱、XamlParseException 自引用崩溃、frozen brush 只读异常、tooltip 白底白字等系列问题；新增自定义滚动条样式

### 数字孪生增强（mes-frontend DigitalTwinScene.vue）

- **LOD 分级渲染**: ≤12 台设备精细模型；13~40 台简化模型（约 10 个 Mesh）；>40 台极简模型（约 4 个 Mesh，隐藏标签）— 大规模设备场景帧率显著提升
- **动态增强**: 主轴转速随实时数据联动、设备 LED 呼吸灯、塔灯颜色随状态变化；tooltip 修复（遮挡/白底白字）
- **增量更新**: 数据刷新改为 updateNodes 增量更新而非整场景重建，配合温度趋势 sparkline 面板

### 看板服务新接口（mes-dashboard）

- **设备历史数据**: `GET /api/dashboard/device/{code}/history` 查询 InfluxDB 时序历史
- **设备批量创建**: `POST /api/dashboard/device/batch` 批量注册设备
- **InfluxDB 零配置启动**: 连接参数（URL/Token/org/bucket）固化进 application.yml 默认值，支持环境变量覆盖；修复 `LocalDateTime` 序列化（注入 Spring ObjectMapper）与 Flux 查询 pivot 报错（改行式查询）

### 其他修复

- **WPF 跨线程修复**: 模拟器 UI 更新统一走 Dispatcher，消除随机闪退
- **仓库清理**: 移除 310MB+ 构建产物（Maven target / .NET bin,obj / 前端 dist / Python __pycache__）、10MB 运行日志、遗留的根目录 aedes node_modules 与 Python .venv（均已在 .gitignore 覆盖，git 仓库本身零冗余）

---

## v1.0.42 (2026-08-10)

### 设备数据链路完善（MQTT → Kafka → 看板全链路打通）

- **docker-compose 补齐 EMQX**: 此前基础设施缺少 MQTT Broker（README 架构图有但 compose 无），MQTT 设备接入链路本地无法跑通 — 新增 `emqx/emqx:5.8.3` 服务（端口 1883 + 18083 控制台，默认 admin/public，内存限制 256M）
- **WPF 模拟器双通道发布**: 新增 MQTT 服务器配置输入（默认 localhost:1883），连接 API 时同步连接 EMQX；模拟数据每 2 秒同时推送 HTTP（`POST /api/dashboard/device/simulate`）+ MQTT（topic `mes/device/{deviceCode}/data`，采用网关协议结构）；MQTT 连接失败不阻断，降级仅走 HTTP
- **Kafka 数据消费者字段解析修复**: `KafkaDeviceDataConsumer` 原来只读 `params` 嵌套结构，而 .NET 网关转发的是 `data` 嵌套结构（`{deviceId, timestamp, dataType, data:{temperature, speed}}`），导致遥测数据全部被丢弃、设备状态永不更新 — 已兼容 `data`/`params` 嵌套 + 平铺三种结构，`deviceId` 缺省时回退 `deviceCode`
- **Kafka 告警消费者新增**: `mes-alarm-event` 此前无消费者（死信 topic），网关转发的设备状态变更全部丢失 — 新增 `KafkaAlarmEventConsumer`，ALARM 状态写入 `dash_alarm_event` 并同步 `dash_device_status`；兼容多格式时间解析（epoch/ISO/DateTime）
- **init.sql 补齐告警表**: `dash_alarm_event` 此前仅存在于 V5 迁移脚本，全新安装（CI 只执行 init.sql）后 `/alarm` 接口报表不存在 — 已补入 init.sql 并在真实 MySQL 验证全量执行通过

### 文档与配置

- **docker-daemon.json**: 清理 4 个已停服的国内镜像源（USTC 2024 关闭、163/百度/docker-cn 早已停服），恢复 Docker Hub 直连，修复镜像拉取失败
- **README 全面升级**: Hero 区（居中标题 + tagline + 徽章 + skillicons 技术 logo）、目录导航、核心指标统计卡、特性表格化、界面预览分组排版、一键启动优先、文档目录表、Star 鼓励页脚 — 移除全部 emoji
- **架构图升级为 Mermaid**: README 整体架构图（6 层 + 分层配色 + 补全看板/AI 服务与消息链路）与 DATABASE 表关系图（erDiagram）替代 ASCII 图
- **README 截图恢复**: 三张项目截图移入仓库 `docs/screenshots/` 相对路径引用（原 user-attachments 外部链接 404）
- **开源规范文件**: 新增 CONTRIBUTING.md / CODE_OF_CONDUCT.md / SECURITY.md / Issue 模板 / PR 模板；CI 分支修复（main→master）+ 数据库初始化验证步骤；仓库添加 10 个 topics；发布首个 Release v1.0.41

---

## v1.0.41 (2026-08-08)

### 构建与运行时修复（mes-frontend）

- **系统设置页 vite 构建失败**: `SettingsView.vue` 中 `saveSettings` 被重复声明两次（主题切换保存 + 保存按钮提交各一份，UI 优化时合并遗漏），`vue-tsc` 类型检查未报错但 `vite build` 编译期直接失败 — 已删除冗余声明（保留主题切换自动保存逻辑）
- **角色管理页运行时空白**: `RoleView.vue` 使用了 `reactive` 但 import 仅含 `ref/computed/onMounted`，setup 运行时 `ReferenceError` 导致页面空白（与 v1.0.40 BOM 页同类根因，均为 `vue-tsc` 全局类型声明漏检）— 已补全导入

### 安全修复（mes-auth / sql）

- **日志泄露明文密码**: `AuthService` 登录失败日志原为 `"密码验证失败 - 输入: {}, 存储: {}"`，将用户明文密码与存储密码（明文或 BCrypt）直接写入日志 — 已改为仅记录用户名
- **init.sql 伪 BCrypt hash**: 种子用户密码 hash 为拼接的无效值，实测 `admin123` 无法通过 BCrypt 校验，全新安装将无法登录 — 已替换为真实生成的 `$2a$10$` hash 并验证匹配
- **明文密码存储降级**: `V9__schema_fix.sql` 种子用户 INSERT 与第 8 步全量 UPDATE、`fix_password.sql` 均以明文 `admin123` 落库（依赖登录逻辑的明文兜底分支）— 已统一为 BCrypt hash，消除明文存储；AuthService 明文兜底分支保留以兼容存量库

---

## v1.0.40 (2026-08-02)

### 前端 UI 全系统统一优化

#### 5 页面主题模式对齐（角色/菜单/权限/设置/个人中心）

- **角色管理 (RoleView)**: 全量重写 — 3 统计卡(总/启用/禁用,点击筛选) + 骨架加载 + 卡片网格(admin/manager/user/qc/engineer 五色图标) + 权限树弹窗保存修复(半选父节点改用 `getCheckedKeys()+getHalfCheckedKeys()` 不再丢失)
- **菜单管理 (MenuView)**: 全量重写 — tree 表格 + 菜单图标渲染 + cell-code + 父级选择编辑弹窗
- **权限管理 (PermissionView)**: 全量重写 — tree 表格 + 类型标签(MENU/BUTTON/API 三色药丸) + 权限类型下拉编辑弹窗
- **系统设置 (SettingsView)**: header 对齐主题 + 新增保存按钮(此前仅主题切换触发持久化,其他开关均不会保存) + `v-loading` 加载态
- **个人中心 (ProfileView)**: header 对齐主题 + 死按钮修复(编辑→接入 `updateProfile` API + 编辑弹窗) + `User` 图标缺导入补全

#### 之前批次（生产线/工位/物料/BOM）

- **生产线/工位**: 统计卡 + filter-bar + 骨架 + 主题表格
- **物料管理**: filter-bar + 骨架 + 空态文案
- **BOM 管理**: filter-bar + 骨架 + 空态文案 + BOM 行项物料下拉选择 + scrap_rate 精度修正
- **BOM 空白 Bug 修复**: `computed` 未导入导致 setup 运行时 `ReferenceError` 页面空白,根因 `vue-tsc` 全局类型声明漏检 — 已建无头 Chrome CDP 全路由运行时验证流程

### 数据库 Schema 补齐

- **`sys_user` 补齐 8 缺失列**: nickname, avatar, employee_no, department, position, manager_id, hire_date, role_id（此前只有 admin 单用户且缺列致 `/auth/info` 报 5004）
- **`sys_permission` 补齐 `icon` 列**
- **`sys_role_permission` 补齐 `sort` 列**
- **`sys_user.uk_username` 修复**: 改为 `(username, deleted)` 复合唯一键,消除软删除后重新创建同名用户冲突
- **补充种子用户**: zhangsan/lisi/wangwu/zhaoliu 4 人,统一密码 `admin123`
- **`init.sql` 完善**: 所有表定义更新为完整规范 Schema（含 deleted_time/deleted_by/version/role_id/sort/icon/uk 修复）
- **V9 迁移脚本**: 新增 `V9__schema_fix.sql`,使用存储过程实现幂等 ALTER（安全重复执行）

---

## v1.0.39 (2026-08-02)

### Redis 企业级深度落地（认证安全 / 分布式序号 / AI 缓存 / 设备在线）

#### AI 服务 Redis 缓存 + 限流（mes-ai-service）

- **统一封装**: 新增 `redis_store.py`（连接超时 1s、`decode_responses`、全部操作带降级），Redis 不可用时自动回退 MySQL/直连，不影响业务
- **分析历史缓存**: zset 缓存最近 50 条（key `analysis:recent:{user}:{device}`，TTL 300s），保存/删除双写同步，列表优先读缓存、空或异常回查 MySQL 防掩盖新增
- **LLM 结果缓存**: 内容寻址（消息+上下文+历史+模型 hash 前 32 位），命中直接复用，成功结果缓存 1 小时
- **LLM 限流**: 滑动窗口 INCR+TTL 60s，超限返回"服务繁忙"（可配置 `rate_limit`，默认 60 次/分钟）

#### 认证安全增强（mes-auth）

- **Token 黑名单**: 登出/改密后将 token SHA-256 写入 `auth:blacklist:{hash}`，TTL=剩余有效期；`JwtAuthFilter` 校验时命中即 401（新增 `POST /auth/logout`）
- **登录失败锁定**: 连续失败 5 次锁定 15 分钟（`auth:fail:{username}`，INCR+TTL），锁定后正确密码也被拒绝，登录成功自动清零
- **降级原则**: Redis 不可用时跳过黑名单/锁定（不阻断登录），不影响原流程；`JwtUtils` 新增 `getRemainingMillis`

#### 工单号分布式生成（mes-workorder）

- **并发唯一序号**: `wo:seq` Redis INCR 生成 `WO+yyyyMMddHHmmss+4位序号`，替代毫秒时间戳（并发创建同毫秒会撞号）；Redis 不可用时降级回时间戳
- **报工原子化**: `completed_quantity` 改为 SQL 原子自增（`+=`），修复并发报工读改写竞态，并重新读取最新进度判断是否转待质检

#### 设备在线心跳 Redis（mes-device-gateway）

- **心跳写入**: 每收到设备数据/状态消息，写入 `device:online:{deviceId}`（值含时间+状态，TTL 90s），Redis 连接失败自动降级不影响网关主流程
- **配置**: `RedisServer/RedisPort/RedisPassword/DeviceHeartbeatTtlSeconds`（appsettings + 环境变量），新增 `StackExchange.Redis` 依赖

#### 基础设施

- **docker-compose**: Redis 内存 128M → 256M，新增 `--maxmemory 256mb --maxmemory-policy allkeys-lru --appendonly yes`（内存满自动淘汰最久未用，防止 OOM 拒写）
- **错误码**: 新增 `USER_LOCKED(5005)`

> **注**: 本版本冒烟测试发现的 sys_user 缺失列问题已在 v1.0.40 通过 V9 迁移修复,详见上方。

---

## v1.0.38 (2026-08-02)

### 设备分析体系企业级升级（SPC / 能耗优化 / 分析历史）

#### AI 分析历史 MySQL 持久化

- **数据表**: 新增 `ai_analysis_history`（V8 迁移），按 `user_id` + `device_code` 隔离
- **后端**: `conversation_store.py` 新增 `save_analysis` / `list_analyses` / `delete_analysis`；Agent 路由新增 `POST/GET /api/v1/agent/analysis` + `DELETE /api/v1/agent/analysis/{id}`（校验 user_id 防越权）
- **前端**: 设备页挂载/打开面板时自动加载历史；历史记录支持**删除**（悬停按钮，MySQL + localStorage + 内存三级同步）
- **按设备隔离**: 每台设备只显示自己的历史记录（后端 `device_code` 过滤 + 前端 `filteredHistory` 双保险）
- **存储降级**: MySQL 不可用时自动降级 localStorage，刷新不丢失

#### 能耗优化 — 企业级实现

- **真实数据接入**: 从 `dash_device_status` 读取真实遥测（温度/转速/状态/设备类型），查不到才退回请求参数
- **四维优化策略**: 参数调优（工艺约束网格搜索）+ 削峰填谷（峰 1.15 / 平 0.73 / 谷 0.38 元分时电价）+ 待机管理（自动断电）+ 预防性维护
- **财务测算**: 月省电量/成本、年化节省、CO₂ 减排（0.581kg/kWh）、投资回本周期
- **实施路线图**: P1 快速见效（1-2周）→ P2 系统优化（1-3月）→ P3 持续改善（3-6月），含验收 KPI
- **前端看板**: KPI 四宫格、策略构成条形图、参数对比表、峰平谷分时电价卡片、路线图时间线、风险提示

#### SPC 统计分析 — 企业级实现

- **真实规格限**: 从 `proc_parameter` 读取工艺参数 LSL/USL/目标值，无则 6σ 窗口估算
- **完整 Western Electric 8 条规则**（ISO 8258）: 超限/连续9点同侧/6点趋势/14点交替/2σ警戒/1σ倾向/分层过稳/混合偏移
- **全过程能力**: CP/CPK + PP/PPK（长期）+ Cpm（目标）+ CPK 90% 置信区间 + 能力等级
- **正态性检验**: Jarque-Bera（偏度/峰度），控制图类型推荐（I-MR / Xbar-R）
- **5M1E 建议**: 按命中规则类型分组生成（机/料/法/环/测），含监控抽样计划
- **前端可视化**: SVG 控制图（六条参考线 + 超限点标红）、直方图、规则检测网格、命中详情

#### 数字孪生 AI 集成

- **新增 4 个 Agent 工具**: `get_device_digital_twin` / `get_all_device_health` / `get_device_alarms` / `get_device_trend`
- **设备页 URL 参数**: `?device=DEV-001` 自动选中设备并切到 3D 视图
- **AI 消息联动**: 回复中的 `DEV-XXX` 自动转为 3D 视图链接
- **分析面板重设计**: 历史记录 + 分类型面板（SPC/能耗/产能/AI建议）+ 专属历史 + 设备卡片

#### 交互与修复

- **设备列表入口**: 设备卡片/详情弹窗新增 SPC分析、能耗优化、AI建议 快捷按钮
- **3D 标题修复**: 点击"能耗优化"等按钮后面板标题跟随入口类型（此前恒显示"AI建议"）
- **AI 建议排版**: Markdown 全元素美化（标题/表格/列表/代码块/引用/链接）
- **温度精度**: 模拟器发送四舍五入 + 前端统一 1 位小数显示（`87.44609665427511` → `87.4`）

---

## v1.0.37 (2026-07-28)

### AI 生产助理 — 离线提醒 + Python 3.12 兼容 + UI 升级

#### AI 服务离线提醒

- **自动检测**: 组件挂载时 ping `/ai/api/v1/agent/tools` 检测 AI 服务状态
- **横幅提醒**: AI 服务离线时，聊天区域顶部显示红色横幅，附带启动命令和重试按钮
- **自动恢复**: 发送消息成功后自动设回在线状态，横幅消失（过渡动画）
- **替代静默错误**: 不再静默失败，用户一眼可见服务状态

#### Python 3.12 兼容性

- `numpy` 版本约束从 `==1.26.4` 放宽到 `>=1.26,<3.0`（Python 3.12 移除 `np.long`）
- `scikit-learn` 版本约束从 `==1.5.2` 放宽到 `>=1.5`
- 新增 `pymysql>=1.1.0` 依赖用于 MySQL 连接

#### UI 全面升级

- **图标库统一**: 所有内联 SVG 替换为 Element Plus 图标组件（`MagicStick`, `Delete`, `Close`, `ArrowRight`, `CircleCheck`, `CircleClose`, `User`）
- **欢迎卡片**: 欢迎消息使用 4 色图标卡片渲染（Monitor / Warning / Document / Notebook），替代 Markdown 小黑点
- **侧边栏能力卡片**: emoji 表情替换为 Element Plus 图标（`Monitor`, `Warning`, `Document`, `Notebook`, `TrendCharts`, `Setting`）
- **双栏页面布局**: 页面模式左侧显示 Agent 介绍 + 能力 + 聊天记录列表，右侧对话区

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