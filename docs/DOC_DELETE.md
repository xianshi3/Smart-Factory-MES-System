# MES 系统删除功能设计文档

## 文档概述

本文档详细描述智能工厂MES系统中删除功能的设计与**完整实现流程**。

---

## 一、准备工作

### 1.1 技术选型说明

| 技术 | 用途 | 说明 |
|------|------|------|
| **MySQL** | 存储删除字段 | 所有业务数据存在 MySQL，删除字段也添加到此 |
| **Redis** | 不需要 | 删除功能不涉及 Redis，MyBatis-Plus 已支持逻辑删除 |
| **Docker** | 运行 MySQL | 项目使用 Docker 运行 MySQL 容器 |

### 1.2 前置条件

在开始实现删除功能前，确保以下服务正常运行：

```bash
# 启动 Docker（如果未启动）
docker compose up -d

# 确认 MySQL 容器运行中
docker ps
# 应该看到 mes-mysql 容器状态为 "Up"
```

---

## 二、数据库操作（MySQL）

### 2.1 连接 MySQL 容器

**方法一：使用 docker exec（推荐）**

```bash
# 进入 MySQL 容器并登录
docker exec -it mes-mysql mysql -uroot -proot
```

**方法二：使用外部客户端**

如果需要用 Navicat、DBeaver 等工具连接：

```
Host: localhost
Port: 3306
Username: root
Password: root
Database: mes_db
```

### 2.2 查看当前数据库表

登录 MySQL 后：

```sql
-- 查看所有表
SHOW TABLES;

-- 会看到以下业务表：
-- wo_work_order        (工单表)
-- wo_work_report       (报工记录)
-- proc_template        (工艺模板)
-- proc_parameter       (工艺参数)
-- qms_quality_record  (质检记录)
-- qms_traceability    (追溯记录)
-- sys_user            (用户表)
-- dash_device_status  (设备状态)
```

### 2.3 添加缺失的字段（deleted_time, deleted_by）

**注意**：init.sql 只包含 `deleted` 字段，需要额外添加 `deleted_time` 和 `deleted_by`。

```sql
USE mes_db;

-- 工单表 wo_work_order
ALTER TABLE wo_work_order 
ADD COLUMN deleted_time DATETIME COMMENT '删除时间',
ADD COLUMN deleted_by BIGINT COMMENT '删除人ID';

-- 报工记录表 wo_work_report
ALTER TABLE wo_work_report 
ADD COLUMN deleted_time DATETIME COMMENT '删除时间',
ADD COLUMN deleted_by BIGINT COMMENT '删除人ID';

-- 工艺模板表 proc_template
ALTER TABLE proc_template 
ADD COLUMN deleted_time DATETIME COMMENT '删除时间',
ADD COLUMN deleted_by BIGINT COMMENT '删除人ID';

-- 工艺参数表 proc_parameter
ALTER TABLE proc_parameter 
ADD COLUMN deleted_time DATETIME COMMENT '删除时间',
ADD COLUMN deleted_by BIGINT COMMENT '删除人ID';

-- 质检记录表 qms_quality_record
ALTER TABLE qms_quality_record 
ADD COLUMN deleted_time DATETIME COMMENT '删除时间',
ADD COLUMN deleted_by BIGINT COMMENT '删除人ID';

-- 追溯记录表 qms_traceability
ALTER TABLE qms_traceability 
ADD COLUMN deleted_time DATETIME COMMENT '删除时间',
ADD COLUMN deleted_by BIGINT COMMENT '删除人ID';
```

**或者直接执行 sql/V2__add_delete_fields.sql**

### 2.4 验证字段存在

```sql
-- 查看工单表结构（应该有 deleted, deleted_time, deleted_by 三个字段）
DESC wo_work_order;

-- 应该能看到 deleted 字段
```

### 2.6 同步更新 sql/init.sql

**注意**：init.sql 已经包含 deleted 字段，无需修改。

---

## 三、后端实现（Java）

### 3.1 整体架构说明

```
mes-workorder/          # 工单服务 (端口 8082)
├── entity/             # 实体类
│   └── WorkOrder.java  # 工单实体
├── service/            # 业务逻辑
│   ├── WorkOrderService.java
│   └── impl/WorkOrderServiceImpl.java
├── controller/         # API 接口
│   └── WorkOrderController.java
└── mapper/             # 数据访问
    └── WorkOrderMapper.java

mes-process/            # 工艺服务 (端口 8083)
mes-quality/           # 质量服务 (端口 8084)
```

### 3.2 工单模块实现

#### Step 1: 修改 WorkOrder 实体类

**文件位置**: `mes-workorder/src/main/java/com/mes/workorder/entity/WorkOrder.java`

```java
package com.mes.workorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工单实体
 */
@Data
@TableName("wo_work_order")
public class WorkOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;           // 工单号
    private String productName;       // 产品名称
    private Integer quantity;         // 计划数量
    private String status;            // 状态: DRAFT/PUBLISHED/PRODUCING/COMPLETED/CANCELLED
    private LocalDateTime startTime;  // 开始时间
    private LocalDateTime endTime;    // 结束时间
    
    // ========== 新增删除字段 ==========
    private Integer deleted;          // 0-未删除 1-已删除
    private LocalDateTime deletedTime; // 删除时间
    private Long deletedBy;           // 删除人ID
    // ==================================

    // 公共字段（来自 BaseEntity 或直接定义）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
}
```

#### Step 2: 修改 WorkOrderService 接口

**文件位置**: `mes-workorder/src/main/java/com/mes/workorder/service/WorkOrderService.java`

```java
package com.mes.workorder.service;

import com.mes.common.result.Result;
import com.mes.workorder.dto.CreateWorkOrderDTO;
import com.mes.workorder.dto.UpdateWorkOrderDTO;
import com.mes.workorder.entity.WorkOrder;

public interface WorkOrderService {

    // 现有方法...
    WorkOrder create(CreateWorkOrderDTO dto, Long userId);
    WorkOrder getById(Long id);
    void updateStatus(Long id, UpdateWorkOrderDTO dto);
    
    // ========== 新增删除方法 ==========
    Result<Void> delete(Long id, Long userId);
    // ==================================
}
```

#### Step 3: 修改 WorkOrderServiceImpl 实现类

**文件位置**: `mes-workorder/src/main/java/com/mes/workorder/service/impl/WorkOrderServiceImpl.java`

```java
package com.mes.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mes.common.result.Result;
import com.mes.workorder.entity.WorkOrder;
import com.mes.workorder.mapper.WorkOrderMapper;
import com.mes.workorder.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderMapper workOrderMapper;

    @Override
    public Result<Void> delete(Long id, Long userId) {
        // 1. 查询工单
        WorkOrder order = getById(id);
        if (order == null) {
            return Result.error("工单不存在");
        }
        
        // 2. 检查删除条件：只有 DRAFT 或 CANCELLED 状态可删除
        if (!"DRAFT".equals(order.getStatus()) && !"CANCELLED".equals(order.getStatus())) {
            return Result.error("只有草稿或已取消的工单可删除");
        }
        
        // 3. 逻辑删除（更新 deleted 字段）
        order.setDeleted(1);
        order.setDeletedBy(userId);
        order.setDeletedTime(LocalDateTime.now());
        workOrderMapper.updateById(order);
        
        return Result.ok();
    }
    
    // ... 其他现有方法
}
```

#### Step 4: 修改 WorkOrderController

**文件位置**: `mes-workorder/src/main/java/com/mes/workorder/controller/WorkOrderController.java`

```java
package com.mes.workorder.controller;

import com.mes.common.result.Result;
import com.mes.workorder.entity.WorkOrder;
import com.mes.workorder.service.WorkOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/workorder")
@RequiredArgsConstructor
@Tag(name = "工单管理")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    // ========== 新增删除接口 ==========
    @DeleteMapping("/{id}")
    @Operation(summary = "删除工单")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return workOrderService.delete(id, userId);
    }
    // =================================
    
    // ... 其他现有接口
}
```

#### Step 5: 修改查询逻辑（过滤已删除记录）

**重要**：这步可选，但推荐添加，可以自动过滤已删除的记录。

**方式一：MyBatis-Plus 自动过滤（推荐）**

在实体类的 `deleted` 字段添加 `@TableLogic` 注解，这样所有查询都会自动过滤 `deleted=1` 的记录。

**文件位置**：`mes-workorder/src/main/java/com/mes/workorder/entity/WorkOrder.java`

```java
package com.mes.workorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工单实体
 */
@Data
@TableName("wo_work_order")
public class WorkOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;           // 工单号
    private String productName;       // 产品名称
    private Integer quantity;         // 计划数量
    private String status;            // 状态

    // ========== 删除字段 ==========
    private Integer deleted;          // 0-未删除 1-已删除
    
    // 添加 @TableLogic 注解后，查询会自动过滤 deleted=1 的记录
    @TableLogic
    public Integer getDeleted() {
        return deleted;
    }
    // ==============================

    private LocalDateTime deletedTime;  // 删除时间
    private Long deletedBy;              // 删除人ID
    
    // ... 其他字段
}
```

**完整示例**：

```java
package com.mes.workorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("wo_work_order")
public class WorkOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private String productName;
    private Integer quantity;
    private String status;
    private LocalDateTime planStartTime;
    private LocalDateTime planEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private String remark;
    private Long createBy;
    private Long issueBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 删除相关字段
    private Integer deleted;
    private LocalDateTime deletedTime;
    private Long deletedBy;
    private Integer version;

    // 重点：添加 @TableLogic 注解
    // 这样查询时自动 WHERE deleted = 0
    @TableLogic
    public Integer getDeleted() {
        return deleted;
    }
}
```

**验证方式**：

添加 `@TableLogic` 后，测试查询是否自动过滤：

```java
// Service 中查询
List<WorkOrder> list = workOrderMapper.selectList(null);
// 实际执行的 SQL 会自动加上 WHERE deleted = 0
// SELECT * FROM wo_work_order WHERE deleted = 0
```

---

**方式二：手动在查询条件中添加（备选）**

如果不使用 `@TableLogic` 注解，可以在每个查询方法中手动添加条件。

**文件位置**：`mes-workorder/src/main/java/com/mes/workorder/service/impl/WorkOrderServiceImpl.java`

```java
@Override
public PageResult<WorkOrder> queryPage(int current, int size, String status, String keyword) {
    // 手动添加 deleted = 0 条件
    LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(WorkOrder::getDeleted, 0);  // 只查询未删除的
    
    if (status != null && !status.isEmpty()) {
        wrapper.eq(WorkOrder::getStatus, status);
    }
    
    if (keyword != null && !keyword.isEmpty()) {
        wrapper.like(WorkOrder::getOrderNo, keyword)
               .or()
               .like(WorkOrder::getProductName, keyword);
    }
    
    wrapper.orderByDesc(WorkOrder::getCreateTime);
    
    IPage<WorkOrder> page = new Page<>(current, size);
    return PageResult.of(workOrderMapper.selectPage(page, wrapper));
}
```

**对比**：

| 方式 | 优点 | 缺点 |
|------|------|------|
| @TableLogic | 自动过滤，代码简洁 | 所有查询都自动过滤 |
| 手动添加 | 灵活控制 | 每个查询都要加条件 |

**推荐使用方式一**，只需在 Entity 的 getter 上加一个注解即可。

---

### 3.3 工艺模块实现

参考工单模块，按以下顺序修改：

1. **修改 ProcessTemplate 实体类** - 添加 deleted, deletedTime, deletedBy 字段
2. **修改 ProcessTemplateService 接口** - 添加 delete 方法
3. **修改 ProcessTemplateServiceImpl** - 实现删除逻辑
4. **修改 ProcessTemplateController** - 添加 DELETE 路由

**工艺模块特殊逻辑**：

```java
@Override
public Result<Void> delete(Long id, Long userId) {
    ProcessTemplate template = getById(id);
    if (template == null) {
        return Result.error("模板不存在");
    }
    
    // 检查状态：只有 DRAFT 可删除
    if (!"DRAFT".equals(template.getStatus())) {
        return Result.error("只有草稿状态的模板可删除");
    }
    
    // 逻辑删除
    template.setDeleted(1);
    template.setDeletedBy(userId);
    template.setDeletedTime(LocalDateTime.now());
    updateById(template);
    
    // 级联删除：同时删除关联的参数
    processParameterMapper.deleteByTemplateId(id);
    
    return Result.ok();
}
```

### 3.4 质量模块实现

**质量记录不允许删除**：

```java
@Override
public Result<Void> delete(Long id, Long userId) {
    return Result.error("质量记录不允许删除，确保数据可追溯");
}
```

---

## 四、前端实现（Vue）

### 4.1 项目结构

```
mes-frontend/
├── src/
│   ├── api/                    # API 接口
│   │   ├── workorder.ts        # 工单 API
│   │   ├── process.ts          # 工艺 API
│   │   └── quality.ts          # 质量 API
│   ├── views/                  # 页面
│   │   ├── workorder/          # 工单页面
│   │   │   └── WorkOrderView.vue
│   │   ├── process/            # 工艺页面
│   │   └── quality/            # 质量页面
│   └── utils/
│       └── delete.ts           # 通用删除工具
```

### 4.2 创建通用删除组件

**文件**: `mes-frontend/src/utils/delete.ts`

```typescript
import { ElMessageBox, ElMessage } from 'element-plus'

/**
 * 通用删除确认弹窗
 * @param title - 删除项名称（如：工单号、产品名称）
 * @param onConfirm - 确认删除后的回调函数
 */
export function confirmDelete(title: string, onConfirm: () => void): void {
  ElMessageBox.confirm(
    `确定要删除「${title}」吗？此操作不可撤销。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      draggable: true,
    }
  )
    .then(() => {
      onConfirm()
    })
    .catch(() => {
      ElMessage.info('已取消删除')
    })
}

/**
 * 通用删除 API 调用
 * @param apiFunc - 删除 API 函数
 * @param successMsg - 成功提示消息
 */
export async function handleDelete(
  apiFunc: () => Promise<any>,
  successMsg: string = '删除成功'
): Promise<boolean> {
  try {
    await apiFunc()
    ElMessage.success(successMsg)
    return true
  } catch (error: any) {
    const msg = error?.response?.data?.message || error?.message || '删除失败'
    ElMessage.error(msg)
    return false
  }
}
```

### 4.3 添加 API 方法

**文件**: `mes-frontend/src/api/workorder.ts`

```typescript
import request from './index'

// 现有 API...
export function getWorkOrderList(params: any) {
  return request.get('/workorder/page', { params })
}

// ========== 新增删除 API ==========
export function deleteWorkOrder(id: number) {
  return request.delete(`/workorder/${id}`)
}
// ================================
```

### 4.4 在页面添加删除按钮

**文件**: `mes-frontend/src/views/workorder/WorkOrderView.vue`

```vue
<template>
  <!-- 在操作列添加删除按钮 -->
  <el-table-column label="操作" width="150" fixed="right">
    <template #default="{ row }">
      <el-button type="primary" size="small" @click="handleEdit(row)">
        编辑
      </el-button>
      <el-button 
        type="danger" 
        size="small" 
        @click="handleDelete(row)"
        :disabled="!canDelete(row.status)"
      >
        删除
      </el-button>
    </template>
  </el-table-column>
</template>

<script setup lang="ts">
import { getWorkOrderList, deleteWorkOrder } from '@/api/workorder'
import { confirmDelete, handleDelete } from '@/utils/delete'

// 判断是否可以删除（只有 DRAFT 或 CANCELLED 可删除）
const canDelete = (status: string) => {
  return status === 'DRAFT' || status === 'CANCELLED'
}

// 处理删除
const handleDelete = (row: any) => {
  confirmDelete(row.orderNo, async () => {
    const success = await handleDelete(() => deleteWorkOrder(row.id))
    if (success) {
      loadData() // 刷新列表
    }
  })
}

// 加载数据
const loadData = async () => {
  // ... 现有逻辑
}
</script>
```

---

## 五、Docker 容器说明

### 5.1 容器需要修改吗？

**不需要修改 Docker 容器本身**。

删除功能只涉及：
1. 数据库表结构（添加字段）- 通过 SQL 语句
2. 后端代码（Java）- 修改源代码
3. 前端代码（Vue）- 修改源代码

### 5.2 容器重启后字段还在吗？

**在**，因为：
- 添加的字段存储在 MySQL 数据卷 `mysql-data` 中
- 即使容器重启或删除重建，数据仍然保留
- 只要不删除数据卷，字段永久存在

### 5.3 重置数据库

如果需要重新初始化数据库：

```bash
# 停止并删除容器（保留数据卷）
docker compose down

# 重新启动并初始化
docker compose up -d

# 重新执行 SQL（因为之前的字段在数据卷中，需要重新添加）
# 或者删除数据卷后再启动：
docker compose down -v  # -v 会删除数据卷
docker compose up -d
```

---

## 六、完整实现清单

### 6.1 数据库操作

| 序号 | 操作 | 状态 | 说明 |
|------|------|------|------|
| 1 | 检查数据库字段 | ✅ 已完成 | init.sql 已包含 deleted 字段 |
| 2 | 无需执行 ALTER | ✅ 已确认 | 所有业务表已有删除字段 |

### 6.2 后端 Java

| 序号 | 操作 | 文件位置 | 状态 |
|------|------|----------|------|
| 1 | 修改 WorkOrder 实体 | mes-workorder/.../entity/WorkOrder.java | ⬜ |
| 2 | 修改 WorkOrderService | mes-workorder/.../service/WorkOrderService.java | ⬜ |
| 3 | 修改 WorkOrderServiceImpl | mes-workorder/.../service/impl/WorkOrderServiceImpl.java | ⬜ |
| 4 | 修改 WorkOrderController | mes-workorder/.../controller/WorkOrderController.java | ⬜ |
| 5 | 修改 ProcessTemplate 实体 | mes-process/.../entity/ProcessTemplate.java | ⬜ |
| 6 | 修改 ProcessTemplateService | mes-process/.../service/ProcessTemplateService.java | ⬜ |
| 7 | 修改 ProcessTemplateServiceImpl | mes-process/.../service/impl/ProcessTemplateServiceImpl.java | ⬜ |
| 8 | 修改 ProcessTemplateController | mes-process/.../controller/ProcessTemplateController.java | ⬜ |

### 6.3 前端 Vue

| 序号 | 操作 | 文件位置 | 状态 |
|------|------|----------|------|
| 1 | 创建 delete.ts 工具 | mes-frontend/src/utils/delete.ts | ⬜ |
| 2 | 添加工单删除 API | mes-frontend/src/api/workorder.ts | ⬜ |
| 3 | 添加工艺删除 API | mes-frontend/src/api/process.ts | ⬜ |
| 4 | 工单页面添加删除按钮 | mes-frontend/src/views/workorder/WorkOrderView.vue | ⬜ |
| 5 | 工艺页面添加删除按钮 | mes-frontend/src/views/process/ProcessView.vue | ⬜ |

### 6.4 测试验证

| 序号 | 测试场景 | 预期结果 | 状态 |
|------|----------|----------|------|
| 1 | 删除草稿状态工单 | 删除成功 | ⬜ |
| 2 | 删除已取消状态工单 | 删除成功 | ⬜ |
| 3 | 删除生产中工单 | 返回错误：不可删除 | ⬜ |
| 4 | 删除已发布工艺模板 | 返回错误：不可删除 | ⬜ |
| 5 | 删除草稿工艺模板 | 删除成功 | ⬜ |
| 6 | 删除质量记录 | 返回错误：不允许删除 | ⬜ |
| 7 | 查询已删除工单 | 不显示（被过滤） | ⬜ |

---

## 七、常见问题

### Q1: 删除字段添加错了怎么办？

**撤销添加的字段：**

```sql
-- 进入 MySQL 容器
docker exec -it mes-mysql mysql -uroot -proot

USE mes_db;

-- 撤销工单表添加的字段
ALTER TABLE wo_work_order 
DROP COLUMN deleted,
DROP COLUMN deleted_time,
DROP COLUMN deleted_by;

-- 撤销工艺表添加的字段
ALTER TABLE proc_template 
DROP COLUMN deleted,
DROP COLUMN deleted_time,
DROP COLUMN deleted_by;

-- 撤销工艺参数表添加的字段
ALTER TABLE proc_parameter 
DROP COLUMN deleted,
DROP COLUMN deleted_time,
DROP COLUMN deleted_by;

-- 撤销质检记录表添加的字段
ALTER TABLE qms_quality_record 
DROP COLUMN deleted,
DROP COLUMN deleted_time,
DROP COLUMN deleted_by;

-- 撤销追溯记录表添加的字段
ALTER TABLE qms_traceability 
DROP COLUMN deleted,
DROP COLUMN deleted_time,
DROP COLUMN deleted_by;
```

**验证撤销成功：**

```sql
-- 查看表结构确认字段已删除
DESC wo_work_order;
```

### Q2: 删除后数据能恢复吗？

**A**: 可以恢复。因为是逻辑删除，只需将 `deleted` 字段改回 0 即可恢复。

### Q3: 如何彻底删除（物理删除）？

**A**: 不推荐物理删除。可创建"回收站"功能，定期清理超过 30 天的已删除数据。

### Q4: 删除后会影响关联表吗？

**A**: 不会。因为是逻辑删除，查询时自动过滤，不会影响关联查询。

### Q5: 需要重启服务吗？

**A**: 修改 Java 代码后需要重新编译和启动。修改数据库不需要重启服务。

---

## 八、数据库错误恢复指南

### 8.1 常见错误及解决方案

| 错误类型 | 原因 | 解决方案 |
|----------|------|----------|
| 字段名拼写错误 | 手误 | 使用 `ALTER TABLE ... CHANGE` 重命名 |
| 数据类型错误 | 用错类型 | `ALTER TABLE ... MODIFY COLUMN` 修改类型 |
| 重复添加字段 | 已存在又添加 | `DROP COLUMN` 删除后重新添加 |
| 表名写错 | 手误 | 无法直接修改表名，需要重建表 |

### 8.2 字段重命名示例

```sql
-- 如果不小心把 deleted 拼写成了 deletd
ALTER TABLE wo_work_order CHANGE deletd deleted TINYINT DEFAULT 0;
```

### 8.3 修改字段类型示例

```sql
-- 如果不小心把 TINYINT 改成了 VARCHAR
ALTER TABLE wo_work_order MODIFY COLUMN deleted TINYINT DEFAULT 0;
```

### 8.4 查看表结构确认

```sql
-- 查看表的所有字段
DESC wo_work_order;

-- 或使用更详细的格式
SHOW FULL COLUMNS FROM wo_work_order;
```

---

*最后更新: 2026-04-06*