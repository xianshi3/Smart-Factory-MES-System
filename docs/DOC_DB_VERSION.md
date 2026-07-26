# 数据库版本控制

## 当前项目状态

| 版本 | 日期 | 说明 |
|------|------|------|
| V1 | 2026-04-04 | 初始数据库结构 |
| V2 | 2026-04-06 | 添加删除功能字段 |
| V3 | 2026-04-12 | 质检相关表 |
| V4 | 2026-04-13 | 权限增强（角色、菜单、权限表） |
| V5 | 2026-05-02 | 告警事件表 |
| V5.5 | 2026-07-27 | 修复唯一约束复合索引 |

## 迁移规范

```bash
# 每次数据库变更：
# 1. 在 sql/ 目录创建 V{n}__xxx.sql 文件
# 2. 手动执行到数据库
docker exec -it mes-mysql mysql -uroot -proot mes_db < sql/V{n}__xxx.sql
# 3. 更新本文件 + 提交 Git
```

## 备份

```bash
docker exec mes-mysql mysqldump -uroot -proot mes_db > backup_$(date +%Y%m%d).sql
```

*最后更新: 2026-07-27*
