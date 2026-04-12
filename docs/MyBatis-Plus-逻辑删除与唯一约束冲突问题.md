# MyBatis-Plus 逻辑删除与唯一约束冲突问题

## 问题背景

在 Spring Boot + MyBatis-Plus 项目中，使用逻辑删除（`@TableLogic`）时遇到了一个经典但棘手的问题：软删除的记录导致唯一约束冲突，使得相同编码的新记录无法创建。

```
### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: 
Duplicate entry '123' for key 'proc_template.uk_template_code'
```

## 问题根源

### 1. 逻辑删除机制

MyBatis-Plus 的 `@TableLogic` 注解会自动将删除操作转换为 `UPDATE SET deleted = 1`，而不是真正的 `DELETE`。查询时自动过滤 `deleted = 1` 的记录。

```java
// BaseEntity.java
@TableLogic
private Integer deleted;
```

### 2. 唯一约束只关注单列

数据库的唯一约束只检查单列，不考虑 `deleted` 状态：

```sql
-- 原始约束（有问题）
ALTER TABLE proc_template ADD UNIQUE INDEX uk_template_code (template_code);

-- 问题：当 template_code='123' 的记录被软删除后
-- deleted=1 的记录仍然占据着 '123' 这个唯一索引
-- 导致无法插入新的 template_code='123' 的记录
```

### 3. 实际影响

这个问题会导致以下场景失败：
1. 创建新模板时，如果历史上存在过相同编码的模板（已删除），创建会失败
2. 工单、生产线、工作站等所有使用逻辑删除 + 唯一约束的实体都受影响

## 解决方案

### 方案一：修改数据库唯一约束（推荐）

将唯一约束改为复合唯一索引，包含 `deleted` 字段：

```sql
-- 修改前
ALTER TABLE proc_template ADD UNIQUE INDEX uk_template_code (template_code);

-- 修改后：允许一个 active(deleted=0) + 多个 deleted(deleted=1) 记录共存
ALTER TABLE proc_template ADD UNIQUE INDEX uk_template_code (template_code, deleted);
```

**优点**：最简单直接，不需要改代码
**适用场景**：所有使用逻辑删除的表

#### 批量修复脚本

```sql
-- 查看所有受影响的表
SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME 
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = 'mes_db' AND NON_UNIQUE = 0 AND INDEX_NAME != 'PRIMARY';

-- 批量修复（根据实际表名调整）
ALTER TABLE mes_db.wo_work_order DROP INDEX uk_order_no;
ALTER TABLE mes_db.wo_work_order ADD UNIQUE INDEX uk_order_no (order_no, deleted);

ALTER TABLE mes_db.dash_device_status DROP INDEX uk_device_code;
ALTER TABLE mes_db.dash_device_status ADD UNIQUE INDEX uk_device_code (device_code, deleted);

ALTER TABLE mes_db.proc_template DROP INDEX uk_template_code;
ALTER TABLE mes_db.proc_template ADD UNIQUE INDEX uk_template_code (template_code, deleted);
```

### 方案二：代码层面校验（辅助方案）

在 Service 层添加唯一性校验，配合 `last()` 方法绕过 @TableLogic 过滤：

```java
// ProcessTemplateServiceImpl.java
@Override
@Transactional(rollbackFor = Exception.class)
public Long create(CreateTemplateDTO dto) {
    // 检查模板编码是否已存在（包括已删除的记录）
    Long count = processTemplateMapper.selectCount(
        new LambdaQueryWrapper<ProcessTemplate>()
            .eq(ProcessTemplate::getTemplateCode, dto.getTemplateCode())
            .last("AND (deleted = 0 OR deleted IS NULL)")
    );
    if (count > 0) {
        throw new RuntimeException("模板编码 " + dto.getTemplateCode() + " 已存在，请使用不同的编码");
    }
    // ... 继续创建逻辑
}
```

**注意**：这种方案只能起到提前校验的作用，真正的根因还是数据库约束问题。

### 方案三：物理删除（不推荐）

对于已删除的记录，直接物理删除：

```sql
-- 物理删除所有软删除的记录
DELETE FROM proc_template WHERE deleted = 1;
```

**缺点**：
- 丢失历史数据
- 不符合审计要求
- 治标不治本

## 最佳实践总结

### 1. 设计阶段预防

在设计数据库时，使用逻辑删除的表，唯一约束必须包含 `deleted` 字段：

```sql
-- 正确示范
CREATE TABLE proc_template (
    id BIGINT PRIMARY KEY,
    template_code VARCHAR(50),
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_template_code (template_code, deleted)
);

-- 错误示范（早期容易犯的错误）
CREATE TABLE proc_template (
    id BIGINT PRIMARY KEY,
    template_code VARCHAR(50),
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_template_code (template_code)  -- 缺少 deleted
);
```

### 2. 代码规范

- 所有实体类继承 BaseEntity，自动获得逻辑删除能力
- 创建唯一约束时，始终考虑与 deleted 字段的复合索引
- 在 Service 层添加唯一性预检，提升用户体验

### 3. 验证清单

| 检查项 | 状态 |
|--||
| 逻辑删除表是否有唯一约束？ | ☐ |
| 唯一约束是否包含 deleted 字段？ | ☐ |
| 是否存在软删除的历史数据？ | ☐ |
| 创建新记录时是否会触发唯一键冲突？ | ☐ |

## 扩展思考

### 为什么 MyBatis-Plus 不自动处理？

MyBatis-Plus 的 `@TableLogic` 设计初衷是让开发者无感知地使用逻辑删除，但它无法控制数据库层面的约束。唯一约束是数据库 schema 层面的设计，需要开发者自行考虑。

### 其他框架的处理

- **JPA/Hibernate**：通过 `@Where` 注解配合 `@SoftDelete` 实现类似功能
- **Think-Think**：框架内部处理了类似冲突
- **Prisma**：通过 `deletedAt` 字段配合唯一索引

## 总结

本文深入分析了 MyBatis-Plus 逻辑删除与 MySQL 唯一约束冲突的根本原因，并提供了两种解决方案：

1. **修改数据库约束**（推荐）：将单列唯一索引改为复合索引 `(column, deleted)`
2. **代码层面校验**（辅助）：在插入前检查是否存在（包括软删除的记录）

建议在项目设计初期就注意这个问题，避免后期数据积累后的修复成本。


