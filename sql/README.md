# sql — 数据库脚本

MES 系统 MySQL 8.0 脚本集合（库：`mes_db`，字符集 utf8mb4）。

## 文件说明

| 文件 | 说明 |
|------|------|
| `init.sql` | **全新安装入口**：全量最新 schema + 种子数据（5 个账号 admin/zhangsan/lisi/wangwu/zhaoliu，密码均 admin123） |
| `V2~V13` | 旧库升级脚本（按版本号顺序执行） |
| `V10_1__planning_board_demo.sql` | 排产演示数据 |

## 使用

```bash
# 全新安装
mysql -uroot -p123455 --default-character-set=utf8mb4 < sql/init.sql

# 旧库升级（按顺序逐个执行）
mysql -uroot -p123455 --default-character-set=utf8mb4 mes_db < sql/V2__add_delete_fields.sql
# ... V3 ~ V13 ...
```

## 幂等性（v1.0.48 起）

**所有迁移脚本可安全重复执行**：

- V2/V4/V5.5/V9/V10/V11：存储过程条件判断（列/索引存在即跳过）
- V6/V7/V8：`CREATE TABLE IF NOT EXISTS` + 种子去重（`batch_no='INIT-BATCH'` 唯一键 / `NOT EXISTS` 防重复流水）
- V9：已移除"全库重置密码为 admin123"的破坏性语句
- V11：工序种子按 `(template_id, step_no)` 去重

> 已验证：`init.sql + V2~V13` 在全新库顺序执行与重复执行均通过。

## 表结构文档

见 [docs/DATABASE.md](../docs/DATABASE.md)（含 ER 关系、表结构、索引、版本历史）。

*最后更新: 2026-08-15*
