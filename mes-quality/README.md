# mes-quality — 质量服务

质检记录、不良品管理、全程追溯。

## 功能

- **质检记录**：创建（检验类型/结果参数校验）、分页查询、通过/不通过判定（原因必填）、删除
- **检验类型**：IPQC（首检）/ FQC（终检）/ OQC（出货检）；结果：PASSED / FAILED / REWORK
- **全程追溯**：按 SN 查询工艺链路（操作员/物料批次/设备/参数快照时间线）

## 技术栈

Spring Boot 3.2.5 · MyBatis-Plus · Druid · Kafka（质检事件）· Knife4j

## 端口

`8084`

## 核心接口

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/quality/record/page` | 分页查询（类型/结果/关键字） | quality:view |
| GET | `/quality/record/{id}` | 记录详情 | quality:view |
| POST | `/quality/record` | 创建质检记录 | quality:create |
| POST | `/quality/record/{id}/pass` | 判定合格 | quality:create |
| POST | `/quality/record/{id}/fail?reason=` | 判定不合格（原因必填） | quality:create |
| DELETE | `/quality/record/{id}` | 删除记录 | quality:delete |
| GET | `/quality/trace/{sn}` | SN 全程追溯 | quality:view |

## 相关数据表

`qms_quality_record` · `qms_traceability`

## 相关文档

- [数据库设计](../docs/DATABASE.md)
- [AI 助手质量场景（不良趋势/缺陷定位）](../mes-ai-service/README.md)

*最后更新: 2026-08-15*
