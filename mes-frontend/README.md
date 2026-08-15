# mes-frontend — 前端应用

Vue 3 + TypeScript + Vite 构建的 MES 管理端。

## 技术栈

| 组件 | 版本 |
|------|------|
| Vue | 3.5 |
| TypeScript | 5.x |
| Vite | 6.x |
| Element Plus | 2.11 |
| Pinia | 3.x |
| Vue Router | 4.x |
| ECharts + vue-echarts | 6.1 / 8.1 |
| Three.js（数字孪生 3D） | 0.185 |
| Vitest（单元测试） | 3.x |

## 页面清单

| 路由 | 页面 | AI 助手场景 |
|------|------|------------|
| `/dashboard` | 工作台 | — |
| `/device` | 设备监控（列表 + 数字孪生 3D） | 设备健康/告警处置/趋势预测/自动建单 |
| `/alarm` | 报警中心 | 告警分布/根因定位/处置优先级 |
| `/ai-assistant` | AI 生产助理（Agent 对话） | 内置能力卡片 |
| `/workorder` | 工单管理 | 交期风险/进度瓶颈/异常排查/产能负荷 |
| `/planning` | 生产调度看板（甘特图 + 拖拽排产） | 产能负荷/冲突检查/交期优化 |
| `/process` | 工艺管理 | 参数合理性/配置建议/工序优化 |
| `/quality` | 质量管理 | 不良趋势/缺陷定位/改善建议 |
| `/report` | 生产报表 | 报表解读/异常预警/趋势分析 |
| `/bom` `/material` | BOM/物料管理 | — |
| `/line` `/workstation` | 基础数据 | — |
| `/settings` 等 | 系统管理/个人中心 | — |

## 架构要点

- **API 统一拦截器**（`src/api/index.ts`）：Bearer token、401 跳登录（登录页不重复跳）、后端未启动友好提示、HTML 错误页不直出
- **开发代理**：`/api`→网关 9090、`/auth`→8081、`/ai`→8087（rewrite）、`/dashboard`→8085 等；生产 `VITE_API_BASE_URL=/api` 统一走网关
- **权限**：路由守卫 + `v-permission` 指令 + 菜单按权限码过滤
- **WebSocket**：单例 + 引用计数 + 自动重连，同源网关 `/api/ws/dashboard?token=`
- **主题**：亮/暗色切换（Pinia + CSS 变量）

## 快速开始

```bash
npm install
npm run dev        # http://localhost:3000
npm run lint       # ESLint
npm run type-check # vue-tsc
npm test           # Vitest 单测
npm run build      # 生产构建
```

## 目录结构

```
src/
├── api/          # axios 实例 + 各模块 API（services/dashboard/agent/auth/system）
├── stores/       # Pinia（user/permission/theme/aiChat）
├── router/       # 路由 + 守卫
├── components/   # ai/AiAssistant（页面级 AI 助手）、device/DigitalTwinScene（3D）
├── utils/        # auth/websocket/markdown（含单测）
└── views/        # 页面
```

## 相关文档

- [开发指南](../docs/DEVELOPMENT.md)
- [AI 服务](../mes-ai-service/README.md)

*最后更新: 2026-08-15*
