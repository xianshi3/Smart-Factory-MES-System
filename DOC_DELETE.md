# MES 系统删除功能设计文档

## 1. 概述

本文档描述智能工厂MES系统中各模块删除功能的设计与实现方案。

### 1.1 设计原则

| 原则 | 说明 |
|------|------|
| 逻辑删除 | 使用 `deleted` 或 `status` 字段标记删除，不物理删除数据 |
| 级联删除 | 删除主记录时关联删除子记录，或阻止删除（有依赖时） |
| 权限控制 | 只有管理员或创建者可删除 |
| 审计日志 | 记录删除操作的时间、操作人、原因 |

---

## 2. 数据表设计

### 2.1 通用删除字段

在所有业务表中添加以下字段：

```sql
-- 方式一：使用 deleted 字段（推荐）
ALTER TABLE table_name ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '0-未删除 1-已删除';

-- 方式二：使用 status 字段标记
ALTER TABLE table_name ADD COLUMN status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE-正常 DELETED-已删除';
```

### 2.2 各模块表结构

#### 2.2.1 工单模块 (wo_work_order)

```sql
ALTER TABLE wo_work_order ADD COLUMN deleted TINYINT DEFAULT 0;
ALTER TABLE wo_work_order ADD COLUMN deleted_time DATETIME;
ALTER TABLE wo_work_order ADD COLUMN deleted_by BIGINT;
```

#### 2.2.2 工艺模块 (proc_template)

```sql
ALTER TABLE proc_template ADD COLUMN deleted TINYINT DEFAULT 0;
ALTER TABLE proc_template ADD COLUMN deleted_time DATETIME;
ALTER TABLE proc_template ADD COLUMN deleted_by BIGINT;
```

#### 2.2.3 质量模块 (qms_quality_record)

```sql
ALTER TABLE qms_quality_record ADD COLUMN deleted TINYINT DEFAULT 0;
ALTER TABLE qms_quality_record ADD COLUMN deleted_time DATETIME;
ALTER TABLE qms_quality_record ADD COLUMN deleted_by BIGINT;
```

---

## 3. 后端实现

### 3.1 统一删除接口设计

#### 3.1.1 API 规范

| 方法 | 路径 | 说明 |
|------|------|------|
| DELETE | /{module}/{id} | 物理删除（不推荐） |
| POST | /{module}/{id}/soft-delete | 逻辑删除 |

#### 3.1.2 请求参数

```
DELETE /workorder/123
Header: X-User-Id: 1
```

#### 3.1.3 响应格式

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

### 3.2 各模块删除实现

#### 3.2.1 工单服务 (mes-workorder)

**Step 1: 修改 Entity**

```java
// WorkOrder.java
@Data
public class WorkOrder extends BaseEntity {
    private String orderNo;
    private String productName;
    private Integer quantity;
    private String status;  // DRAFT/PUBLISHED/PRODUCING/COMPLETED/CANCELLED
    private Integer deleted;  // 新增
    private Long deletedBy;
    private LocalDateTime deletedTime;
}
```

**Step 2: 修改 Service**

```java
// WorkOrderService.java
public interface WorkOrderService {
    Result<Void> delete(Long id, Long userId);
}

// WorkOrderServiceImpl.java
@Override
public Result<Void> delete(Long id, Long userId) {
    WorkOrder order = getById(id);
    if (order == null) {
        return Result.error("工单不存在");
    }
    
    // 检查状态，只有 DRAFT/CANCELLED 可删除
    if (!"DRAFT".equals(order.getStatus()) && !"CANCELLED".equals(order.getStatus())) {
        return Result.error("只有草稿或已取消的工单可删除");
    }
    
    // 逻辑删除
    order.setDeleted(1);
    order.setDeletedBy(userId);
    order.setDeletedTime(LocalDateTime.now());
    updateById(order);
    
    return Result.ok();
}
```

**Step 3: 修改 Controller**

```java
// WorkOrderController.java
@DeleteMapping("/{id}")
@Operation(summary = "删除工单")
public Result<Void> delete(@PathVariable Long id,
                            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
    return workOrderService.delete(id, userId);
}
```

#### 3.2.2 工艺服务 (mes-process)

```java
// ProcessTemplateServiceImpl.java
@Override
public Result<Void> delete(Long id, Long userId) {
    ProcessTemplate template = getById(id);
    if (template == null) {
        return Result.error("模板不存在");
    }
    
    // 检查状态，只有 DRAFT 可删除
    if (!"DRAFT".equals(template.getStatus())) {
        return Result.error("只有草稿状态的模板可删除");
    }
    
    // 检查是否有工单使用该模板
    if (workOrderMapper.countByTemplateId(id) > 0) {
        return Result.error("该模板已被工单使用，无法删除");
    }
    
    // 逻辑删除
    template.setDeleted(1);
    template.setDeletedBy(userId);
    template.setDeletedTime(LocalDateTime.now());
    updateById(template);
    
    // 同时删除关联的参数
    processParameterMapper.deleteByTemplateId(id);
    
    return Result.ok();
}
```

#### 3.2.3 质量服务 (mes-quality)

```java
// QualityServiceImpl.java
@Override
public Result<Void> delete(Long id, Long userId) {
    // 质量记录不允许删除，只能标记
    return Result.error("质量记录不允许删除");
}
```

---

## 4. 前端实现

### 4.1 统一删除方法

```typescript
// mes-frontend/src/utils/delete.ts

import { ElMessageBox, ElMessage } from 'element-plus'

/**
 * 通用删除确认
 * @param title 删除项名称
 * @param onConfirm 确认回调
 */
export function confirmDelete(title: string, onConfirm: () => void) {
  ElMessageBox.confirm(
    `确定要删除 "${title}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    onConfirm()
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}
```

### 4.2 工单页面删除按钮

```vue
<!-- mes-frontend/src/views/workorder/WorkOrderView.vue -->

<template>
  <el-button type="danger" size="small" @click="handleDelete(row)">
    删除
  </el-button>
</template>

<script setup lang="ts">
import { deleteWorkOrder } from '@/api/workorder'
import { confirmDelete } from '@/utils/delete'
import { ElMessage } from 'element-plus'

const handleDelete = (row: any) => {
  confirmDelete(row.orderNo, async () => {
    try {
      await deleteWorkOrder(row.id)
      ElMessage.success('删除成功')
      loadData()  // 刷新列表
    } catch (error) {
      ElMessage.error(error.message || '删除失败')
    }
  })
}
</script>
```

### 4.3 API 接口定义

```typescript
// mes-frontend/src/api/workorder.ts

import request from './index'

export function deleteWorkOrder(id: number) {
  return request.delete(`/workorder/${id}`)
}
```

---

## 5. 删除规则汇总

### 5.1 各模块删除条件

| 模块 | 可删除状态 | 限制条件 | 级联删除 |
|------|-----------|---------|---------|
| 工单 | DRAFT, CANCELLED | 无 | 报工记录 |
| 工艺 | DRAFT | 无工单使用 | 工艺参数 |
| 质量 | 不允许 | - | - |
| 设备 | ONLINE, OFFLINE | 无生产中工单 | - |
| 用户 | - | 不能删除自己 | 租户关联 |

### 5.2 状态值说明

```
工单状态:
- DRAFT: 草稿（可删除）
- PUBLISHED: 已下发（不可删除）
- PRODUCING: 生产中（不可删除）
- COMPLETED: 已完工（不可删除）
- CANCELLED: 已取消（可删除）

工艺状态:
- DRAFT: 草稿（可删除）
- PUBLISHED: 已发布（不可删除）
```

---

## 6. 实现清单

### 6.1 数据库

- [ ] 在 `sql/init.sql` 中添加 `deleted`, `deleted_by`, `deleted_time` 字段

### 6.2 后端 Java

- [ ] 修改各模块 Entity，添加删除字段
- [ ] 修改 Service，添加 delete 方法和删除校验逻辑
- [ ] 修改 Controller，添加 DELETE 路由
- [ ] 修改 Mapper，查询时过滤已删除记录

### 6.3 前端 Vue

- [ ] 创建 `src/utils/delete.ts` 通用删除确认组件
- [ ] 在各业务页面的表格操作列添加删除按钮
- [ ] 添加对应的 API 方法

### 6.4 测试

- [ ] 测试正常删除流程
- [ ] 测试无法删除的场景（状态限制）
- [ ] 测试级联删除

---

*最后更新: 2026-04-06*