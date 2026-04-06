# 数据库版本记录

## 版本清单

### V2 - 2026-04-06
**添加 deleted_time 和 deleted_by 字段**

init.sql 已包含 `deleted` 字段，但缺少 `deleted_time` 和 `deleted_by`。

| 表名 | 添加字段 |
|------|----------|
| wo_work_order | deleted_time, deleted_by |
| wo_work_report | deleted_time, deleted_by |
| proc_template | deleted_time, deleted_by |
| proc_parameter | deleted_time, deleted_by |
| qms_quality_record | deleted_time, deleted_by |
| qms_traceability | deleted_time, deleted_by |

**执行方法**：
```bash
# 方式1: 手动执行 SQL
docker exec -it mes-mysql mysql -uroot -proot
# 然后执行 sql/V2__add_delete_fields.sql

# 方式2: 直接导入
docker exec -i mes-mysql mysql -uroot -proot mes_db < sql/V2__add_delete_fields.sql
```

---

### V1 - 2026-04-04
**初始数据库结构**

包含以下表（已有 deleted 字段）：
- sys_user, wo_work_order, wo_work_report
- proc_template, proc_parameter
- qms_quality_record, qms_traceability
- dash_device_status, dash_production_stats, dash_oee_data

---

*最后更新: 2026-04-06*