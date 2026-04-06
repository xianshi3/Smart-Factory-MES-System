# 数据库版本记录

## 版本清单

### V2 - 2026-04-06
**删除功能字段**

| 表名 | 添加字段 |
|------|----------|
| wo_work_order | deleted, deleted_time, deleted_by |
| proc_template | deleted, deleted_time, deleted_by |
| proc_parameter | deleted, deleted_time, deleted_by |
| qms_quality_record | deleted, deleted_time, deleted_by |
| qms_traceability | deleted, deleted_time, deleted_by |

**执行方法：**
```bash
docker exec -it mes-mysql mysql -uroot -proot mes_db
```
然后执行 `sql/V2__add_delete_fields.sql` 中的 ALTER 语句。

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
2. 手动执行到数据库
3. 更新本文件
4. 提交 Git

---

*最后更新: 2026-04-06*