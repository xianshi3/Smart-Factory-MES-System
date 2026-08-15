# mes-workorder — 工单与排产服务

工单全生命周期管理 + APS 高级排产（生产调度看板）。

## 功能

### 工单管理（`/workorder`）

- 工单 CRUD：创建（参数校验）、详情、分页查询（状态/关键字）
- 状态流转：下发（ISSUED）→ 开始生产（IN_PRODUCTION）→ 完成（COMPLETED）→ 关闭（CLOSED）
- 报工：`SubmitReportDTO` 参数校验，`completed_quantity` 原子累加（`{0}` 参数化 SQL，防并发竞态）
- 软删除 + 工单号 `WOyyyyMMddHHmmssNNNN` 生成

### 生产调度看板（`/workorder/planning`）

- 甘特看板：设备/工序级任务 + 时间窗口 + 冻结/已下发标记
- 自动排程（APS）：优先级 + 交期 + 工序拆分 + 负载均衡 + 工作日历（mes_shift/mes_work_calendar）
- 拖拽排产：换设备/改时间/拉伸，冲突检测（重叠检测 + 冻结拦截）
- 撤销栈：按用户隔离（`ConcurrentHashMap<userId, Deque>`，最多 20 步）
- 冻结/解冻、下发、变更日志

## 技术栈

Spring Boot 3.2.5 · MyBatis-Plus · Druid · Redis · Knife4j

## 端口

`8082`

## 核心接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/workorder/page` | 分页查询（size ≤ 100） | workorder:view |
| GET | `/workorder/{id}` | 工单详情 | workorder:view |
| POST | `/workorder` | 创建工单 | workorder:create |
| PUT | `/workorder/{id}` | 更新工单 | workorder:edit |
| POST | `/workorder/{id}/issue` | 下发 | workorder:edit |
| POST | `/workorder/{id}/start` | 开始生产 | workorder:edit |
| POST | `/workorder/{id}/report` | 报工 | workorder:edit |
| DELETE | `/workorder/{id}` | 删除 | workorder:delete |
| GET | `/workorder/planning/board` | 排产看板数据 | Bearer |
| POST | `/workorder/planning/auto-plan` | 自动排程 | planning:edit |
| POST | `/workorder/planning/move` | 拖拽调整 | planning:edit |
| POST | `/workorder/planning/undo` | 撤销 | planning:edit |
| POST | `/workorder/planning/freeze` | 冻结 | planning:edit |
| POST | `/workorder/planning/release` | 下发排产 | planning:edit |

## 相关数据表

`wo_work_order` · `wo_work_report` · `wo_schedule` · `wo_schedule_log` · `mes_shift` · `mes_work_calendar`

## 相关文档

- [数据库设计](../docs/DATABASE.md)
- [前端页面](../mes-frontend/README.md)

*最后更新: 2026-08-15*
