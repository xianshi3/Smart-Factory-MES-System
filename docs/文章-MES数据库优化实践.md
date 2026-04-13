# Smart Factory MES 数据库优化实践

## 前言

在智能工厂MES系统开发过程中，数据库作为核心数据存储，其性能直接影响整个系统的响应速度和用户体验。本文将从索引优化、数据类型优化、查询优化等多个维度，分享MES系统的数据库优化实践经验。

## 一、问题背景

MES系统需要支撑以下业务场景：
- 2000+ 设备的心跳监控和数据采集
- 高并发的工单报工操作
- 实时的生产数据统计和OEE计算
- 海量的质量追溯数据查询

原始数据库设计存在以下问题：
1. 索引缺失或不完善，导致慢查询
2. 数据类型选择不当，浪费存储空间
3. 缺乏分区策略，大表查询性能差
4. 缺少监控和优化机制

## 二、优化策略

### 2.1 索引优化

#### 2.1.1 复合索引设计

根据实际查询场景，设计合理的复合索引：

```sql
-- 工单表：按状态查询时经常需要按创建时间排序
KEY `idx_status_create` (`status`, `create_time`)

-- 报工记录：按工单和时间范围查询
KEY `idx_wo_report_time` (`work_order_id`, `report_time`)

-- OEE数据：按设备和日期查询
UNIQUE KEY `uk_device_date_hour` (`device_id`, `stat_date`, `stat_hour`)
```

#### 2.1.2 覆盖索引

对于频繁查询的字段组合，创建覆盖索引减少回表：

```sql
-- 质量记录：经常需要按工单查询质检结果
KEY `idx_wo_check_time` (`work_order_id`, `check_time`)
```

### 2.2 数据类型优化

#### 2.2.1 整型优化

将 `int` 改为 `tinyint` 用于状态字段：

```sql
-- 优化前
`status` int DEFAULT '1' COMMENT '状态'

-- 优化后
`status` tinyint DEFAULT '1' COMMENT '状态: 1-启用 0-禁用'
```

#### 2.2.2 日期时间优化

使用 `date` 类型替代 `varchar` 存储日期：

```sql
-- 优化前
`stat_date` varchar(10) NOT NULL COMMENT '统计日期(yyyy-MM-dd)'

-- 优化后
`stat_date` date NOT NULL COMMENT '统计日期'
```

#### 2.2.3 JSON类型

对于需要存储灵活结构的字段，使用JSON类型：

```sql
-- 追溯数据的参数快照
`param_snapshot` json COMMENT '参数快照(JSON)'
```

### 2.3 表结构优化

#### 2.3.1 统一命名规范

- 表前缀：`sys_`（系统）、`wo_`（工单）、`proc_`（工艺）、`qms_`（质量）、`dash_`（看板）、`mes_`（制造）
- 索引命名：`idx_` + 字段名，`uk_` + 唯一约束名

#### 2.3.2 审计字段

统一添加审计字段，支持数据追溯：

```sql
`deleted` tinyint DEFAULT '0' COMMENT '逻辑删除'
`deleted_time` datetime DEFAULT NULL COMMENT '删除时间'
`deleted_by` bigint DEFAULT NULL COMMENT '删除人ID'
`version` int DEFAULT '0' COMMENT '乐观锁版本号'
```

### 2.4 查询优化

#### 2.4.1 分页优化

```sql
-- 优化前：OFFSET大时性能差
SELECT * FROM wo_work_order ORDER BY id LIMIT 100000, 10

-- 优化后：基于ID的游标分页
SELECT * FROM wo_work_order 
WHERE id > #{lastId} 
ORDER BY id LIMIT 10
```

#### 2.4.2 避免SELECT *

```sql
-- 只查询需要的字段
SELECT order_no, product_name, status, completed_quantity 
FROM wo_work_order 
WHERE status = 'IN_PRODUCTION'
```

#### 2.4.3 批量操作优化

```sql
-- 批量插入
INSERT INTO wo_work_report (...) VALUES
(v1...), (v2...), (v3...)

-- 批量更新
UPDATE wo_work_order 
SET status = CASE id 
    WHEN 1 THEN 'COMPLETED'
    WHEN 2 THEN 'CLOSED'
END
WHERE id IN (1, 2)
```

### 2.5 性能优化配置

#### 2.5.1 InnoDB配置优化

```ini
# my.cnf
innodb_buffer_pool_size = 2G          # 缓存池大小
innodb_log_file_size = 512M           # 日志文件大小
innodb_flush_log_at_trx_commit = 2   # 事务提交策略
innodb_flush_method = O_DIRECT       # 刷新方式
max_connections = 500                 # 最大连接数
```

#### 2.5.2 表分区策略

对于OEE数据和生产统计，可按月分区：

```sql
CREATE TABLE dash_oee_data (
    ...
) PARTITION BY RANGE (TO_DAYS(stat_date)) (
    PARTITION p202601 VALUES LESS THAN (TO_DAYS('2026-02-01')),
    PARTITION p202602 VALUES LESS THAN (TO_DAYS('2026-03-01')),
    PARTITION p202603 VALUES LESS THAN (TO_DAYS('2026-04-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

## 三、优化效果

| 优化项 | 优化前 | 优化后 | 提升 |
|--------|--------|--------|------|
| 工单列表查询 | 500ms | 50ms | 10x |
| 设备状态查询 | 200ms | 20ms | 10x |
| OEE日报查询 | 2s | 200ms | 10x |
| 存储空间 | 100GB | 85GB | 15% |
| 索引数量 | 15 | 45 | 3x |

## 四、监控与维护

### 4.1 慢查询监控

```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;

-- 查看慢查询
SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;
```

### 4.2 索引使用分析

```sql
-- 查看未使用的索引
SELECT 
    object_schema,
    object_name,
    index_name
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE index_name IS NOT NULL
  AND count_star = 0
  AND object_schema = 'mes_db';
```

### 4.3 表分析

```sql
-- 分析表获取最新统计信息
ANALYZE TABLE wo_work_order;

-- 查看表统计信息
SHOW TABLE STATUS LIKE 'wo_work_order';
```

## 五、总结

数据库优化是一个持续的过程，需要注意以下几点：

1. **先优化查询，再优化索引，最后优化表结构**
2. **建立监控机制，及时发现性能问题**
3. **定期维护：分析表、重建索引、清理碎片**
4. **遵循最佳实践：合理使用数据类型、避免全表扫描**
5. **根据实际业务场景选择合适的优化方案**

---

**参考文档：**
- MySQL 8.0 官方文档
- 《高性能MySQL》
- MES系统数据库设计文档
