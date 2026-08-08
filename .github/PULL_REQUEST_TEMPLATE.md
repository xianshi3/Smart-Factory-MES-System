## 变更描述

<!-- 简要说明本次变更内容与动机 -->

Closes #<issue_number>

## 变更类型

- [ ] feat: 新功能
- [ ] fix: 缺陷修复
- [ ] docs: 文档更新
- [ ] refactor: 重构
- [ ] perf: 性能优化
- [ ] chore: 构建/工具链

## 提交前检查

- [ ] 后端 `mvn compile` 通过（如改动 Java）
- [ ] 前端 `npm run type-check` 与 `npm run build` 通过（如改动前端）
- [ ] Python `python -m compileall -q src` 通过（如改动 AI 服务）
- [ ] .NET `dotnet build` 通过（如改动设备网关）
- [ ] 数据库 Schema 变更已同步 `sql/` 迁移脚本与 `docs/DATABASE.md`
- [ ] CHANGELOG 已更新（涉及行为变更）

## 测试说明

<!-- 说明本地如何验证了本次变更 -->
