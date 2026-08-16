# 贡献指南 (Contributing Guide)

欢迎为 Virtual Path MES 贡献代码！请阅读以下指南以确保协作顺畅。

## 分支规范

| 分支 | 用途 |
|------|------|
| `master` | 稳定发布分支，仅接受 PR 合并 |
| `develop` | 开发集成分支，日常开发目标 |
| `feat/*` | 功能开发分支，从 `develop` 拉取 |

## 开发流程

1. Fork 本仓库，从 `develop` 创建特性分支：`git checkout -b feat/xxx develop`
2. 遵循现有代码风格与分层规范（Controller → Service → ServiceImpl → Mapper）
3. 本地验证通过后再提交（见下方"提交前检查"）
4. 提交后推送到你的 Fork，并向 `develop` 发起 Pull Request
5. 等待 Review 与 CI 通过后合并

## 提交前检查

```bash
# 后端（全模块编译 + 单元测试）
mvn compile -T 4
mvn test -pl mes-common          # 公共模块单测（JWT/Result）

# 前端（ESLint + 类型检查 + 单元测试 + 生产构建）
cd mes-frontend && npm run lint && npm run type-check && npm test && npm run build

# Python AI 服务（语法检查 + 测试）
cd mes-ai-service && python -m compileall -q src && pytest tests -q

# .NET 设备网关
cd mes-device-gateway && dotnet build
```

> 注意：`vue-tsc` 存在全局类型声明漏检，页面改动请确认 vite 构建（`npm run build`）通过。
> 涉及鉴权/权限改动时，请同步确认网关 `JwtAuthGlobalFilter` 白名单与各控制器 `@RequirePermission` 注解。

## 提交信息规范

采用 [Conventional Commits](https://www.conventionalcommits.org/)：

- `feat: 新功能`
- `fix: 缺陷修复`
- `docs: 文档更新`
- `refactor: 重构`
- `perf: 性能优化`
- `test: 测试`
- `chore: 构建/工具链`

## Issue 与 PR

- Bug 请附：复现步骤、期望行为、实际行为、浏览器/环境信息
- 功能建议请描述：背景、目标、实现思路
- PR 请关联对应 Issue 编号

## 文档更新

涉及行为变更或数据库 Schema 变更时，必须同步更新：

- `docs/CHANGELOG.md`（新增版本条目）
- `README.md`（如影响使用方式）
- `docs/DATABASE.md`（如涉及表结构）
- `sql/`（新增迁移脚本，遵循 `V{n}__description.sql` 命名）
