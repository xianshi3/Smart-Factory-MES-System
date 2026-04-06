# 数据库版本控制设计文档

## 概述

本文档介绍 MES 项目的数据库版本控制方案。

---

## 一、方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **Flyway** | 自动执行、版本追踪 | 需要集成到 Java 项目 | 中大型项目 |
| **Liquibase** | 支持多种数据库、回滚 | 学习曲线较陡 | 复杂变更 |
| **Git 管理 SQL** | 简单直观、无需额外工具 | 手动执行 | 小型项目 |
| **MyBatis-Plus** | 已集成、可配合使用 | 功能有限 | 本项目 |

---

## 二、推荐方案：Git 管理 SQL（最适合本项目）

### 2.1 目录结构

```
sql/
├── V1__init_schema.sql      # 初始化表结构
├── V2__add_delete_fields.sql # 删除功能字段
├── V3__xxxx.sql             # 后续变更
└── README.md                # 版本说明
```

### 2.2 命名规范

```
V{版本号}__{描述}.sql

示例：
V1__init_schema.sql          # V1 初始版本
V2__add_delete_fields.sql    # V2 删除功能
V3__add_user_role.sql        # V3 用户角色
```

### 2.3 版本说明文件

**sql/VERSION.md**

```markdown
# 数据库版本记录

## V2 - 2026-04-06
- 添加删除功能字段
- 影响表：wo_work_order, proc_template, proc_parameter, qms_quality_record, qms_traceability

## V1 - 2026-04-04
- 初始数据库结构
- 包含所有业务表和示例数据
```

---

## 三、Flyway 方案（可选）

如果需要自动化版本控制，可以集成 Flyway。

### 3.1 添加依赖

在 `pom.xml` 中添加：

```xml
<dependencies>
    <!-- Flyway Core -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
        <version>10.4.1</version>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-mysql</artifactId>
        <version>10.4.1</version>
    </dependency>
</dependencies>
```

### 3.2 配置 application.yml

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    baseline-version: 0
```

### 3.3 创建迁移脚本

```
src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__add_delete_fields.sql
└── V3__xxxx.sql
```

---

## 四、手动管理方案（当前使用）

### 4.1 操作流程

```bash
# 1. 每次数据库变更，创建新 SQL 文件
sql/V2__add_delete_fields.sql

# 2. 在 sql/README.md 记录变更内容
# 3. 手动执行 SQL 到数据库
docker exec -it mes-mysql mysql -uroot -proot mes_db < sql/V2__add_delete_fields.sql

# 4. 提交到 Git
```

### 4.2 检查当前数据库版本

```sql
-- 查看所有表
SHOW TABLES;

-- 查看特定表的字段
DESC wo_work_order;

-- 查看数据
SELECT * FROM wo_work_order LIMIT 5;
```

---

## 五、最佳实践

### 5.1 变更前

1. **备份数据库**
   ```bash
   docker exec mes-mysql mysqldump -uroot -proot mes_db > backup_$(date +%Y%m%d).sql
   ```

2. **在测试环境验证**

3. **创建变更脚本**

### 5.2 变更后

1. **验证数据正确**
2. **更新 sql/README.md**
3. **提交到 Git**

### 5.3 回滚流程

```sql
-- 如果需要回滚，手动逆向操作
ALTER TABLE wo_work_order DROP COLUMN deleted;

-- 或使用备份恢复
docker exec -i mes-mysql mysql -uroot -proot mes_db < backup_20260406.sql
```

---

## 六、当前项目状态

### 6.1 已执行的变更

| 版本 | 日期 | 说明 |
|------|------|------|
| V1 | 2026-04-04 | 初始数据库结构 |
| V2 | 2026-04-06 | 添加删除功能字段 |

### 6.2 待执行的变更

- 暂无

### 6.3 后续变更流程

1. 在 `sql/` 目录创建 `V{n}__xxx.sql` 文件
2. 手动执行到数据库
3. 更新 `sql/VERSION.md`
4. 提交 Git

---

*最后更新: 2026-04-06*