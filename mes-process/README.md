# mes-process — 工艺服务

工艺模板、工艺参数、工序步骤的版本化管理。

## 功能

- **工艺模板**：创建（编码唯一）/ 更新 / 分页查询 / 复制为草稿 / 发布 / 删除
- **版本管理**：乐观锁（`version`），已发布模板不可直接修改（需复制为新版本）
- **工艺参数**：参数名/编码/值/上下限/单位，参数合理性校验（`/parameters/check` 实测值比对上下限）
- **工序步骤**：工序定义、排序、工时

## 技术栈

Spring Boot 3.2.5 · MyBatis-Plus · Druid · Redis · Knife4j

## 端口

`8083`

## 核心接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/process/template/page` | 分页查询 | process:view |
| GET | `/process/template/{id}` | 详情（含参数+工序） | process:view |
| POST | `/process/template` | 创建 | process:create |
| PUT | `/process/template/{id}` | 更新 | process:edit |
| POST | `/process/template/{id}/publish` | 发布 | process:edit |
| POST | `/process/template/{id}/copy` | 复制为草稿 | process:create |
| DELETE | `/process/template/{id}` | 删除 | process:delete |
| POST/PUT/DELETE | `/process/template/{id}/parameters` | 参数管理 | process:edit/delete |
| POST/PUT/DELETE | `/process/template/{id}/steps` | 工序管理 | process:edit/delete |
| POST | `/process/template/{id}/parameters/check` | 参数合理性校验 | process:view |

> 状态枚举：`DRAFT`（草稿）/ `PUBLISHED`（已发布）

## 相关数据表

`proc_template` · `proc_parameter` · `proc_step`

## 相关文档

- [数据库设计](../docs/DATABASE.md)
- [AI 助手工艺场景](../mes-ai-service/README.md)

*最后更新: 2026-08-15*
