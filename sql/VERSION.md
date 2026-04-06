# 数据库版本记录

## 版本清单

### V2 - 2026-04-06
**删除功能字段**

**重要发现**：`init.sql` 已包含 `deleted` 字段，无需额外执行 SQL。

| 表名 | 状态 |
|------|------|
| wo_work_order | ✅ init.sql 已包含 |
| wo_work_report | ✅ init.sql 已包含 |
| proc_template | ✅ init.sql 已包含 |
| proc_parameter | ✅ init.sql 已包含 |
| qms_quality_record | ✅ init.sql 已包含 |
| qms_traceability | ✅ init.sql 已包含 |

**结论**：此版本无需执行任何数据库操作。

---

### V1 - 2026-04-04
**初始数据库结构**

包含以下表：
- sys_user (用户表)
- wo_work_order (工单表)
- wo_work_report (报工记录)
- proc_template (工艺模板)
- proc_parameter (工艺参数)
- qms_quality_record (质检记录)
- qms_traceability (追溯记录)
- dash_device_status (设备状态)
- dash_production_stats (生产统计)
- dash_oee_data (OEE数据)

---

## 变更流程

1. 创建新版本 SQL 文件：`sql/V{n}__xxx.sql`
2. 检查字段是否已存在（如 init.sql）
3. 手动执行到数据库（如果需要）
4. 更新本文件
5. 提交 Git

---

## 常见问题

**Q: 出现 "Duplicate column name 'deleted'" 错误**
**A**: 这是因为 init.sql 已经包含了 deleted 字段，无需再次添加。

**Q: 如何检查字段是否存在？**
```sql
DESC wo_work_order;
-- 查看是否有 deleted 字段
```

---

*最后更新: 2026-04-06*