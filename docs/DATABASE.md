# Smart Factory MES System 数据库设计文档

## 数据库概览

- **数据库名**: mes_db
- **字符集**: utf8mb4
- **存储引擎**: InnoDB

---

## 表结构总览

### 1. 用户表 sys_user

| 字段 | 类型 | 说明 | 约束 |
|-----|------|------|------|
| id | bigint | 主键ID | PK, AUTO_INCREMENT |
| username | varchar(50) | 用户名 | NOT NULL, UNIQUE |
| password | varchar(255) | 密码 | NOT NULL |
| real_name | varchar(50) | 真实姓名 | |
| phone | varchar(20) | 手机号 | |
| email | varchar(100) | 邮箱 | |
| status | int | 状态: 1启用 0禁用 | DEFAULT 1 |
| role | varchar(20) | 角色 | DEFAULT 'USER' |
| create_time | datetime | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | datetime | 更新时间 | ON UPDATE CURRENT_TIMESTAMP |
| deleted | int | 逻辑删除 | DEFAULT 0 |
| version | int | 乐观锁 | DEFAULT 0 |

---

### 2. 工单表 wo_work_order

| 字段 | 类型 | 说明 | 约束 |
|-----|------|------|------|
| id | bigint | 主键ID | PK, AUTO_INCREMENT |
| order_no | varchar(50) | 工单编号 | NOT NULL, UNIQUE |
| product_name | varchar(100) | 产品名称 | NOT NULL |
| product_model | varchar(100) | 产品型号 | |
| plan_quantity | int | 计划数量 | NOT NULL |
| completed_quantity | int | 已完成数量 | DEFAULT 0 |
| status | varchar(20) | 状态 | NOT NULL |
| workstation_id | bigint | 工位ID | |
| process_template_id | bigint | 工艺模板ID | |
| priority | varchar(20) | 优先级 | DEFAULT 'MEDIUM' |
| planned_start_time | datetime | 计划开始时间 | |
| planned_end_time | datetime | 计划结束时间 | |
| actual_start_time | datetime | 实际开始时间 | |
| actual_end_time | datetime | 实际结束时间 | |
| remark | varchar(500) | 备注 | |
| create_by | bigint | 创建人ID | |
| issue_by | bigint | 下发人ID | |
| create_time | datetime | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | datetime | 更新时间 | ON UPDATE CURRENT_TIMESTAMP |
| deleted | int | 逻辑删除 | DEFAULT 0 |
| deleted_time | datetime | 删除时间 | |
| deleted_by | bigint | 删除人ID | |
| version | int | 乐观锁 | DEFAULT 0 |

**状态枚举**: CREATED / ISSUED / IN_PRODUCTION / PENDING_QC / COMPLETED / CLOSED

---

### 3. 报工记录表 wo_work_report

| 字段 | 类型 | 说明 | 约束 |
|-----|------|------|------|
| id | bigint | 主键ID | PK, AUTO_INCREMENT |
| work_order_id | bigint | 工单ID | NOT NULL |
| device_id | bigint | 设备ID | |
| operator_id | bigint | 操作员ID | |
| report_quantity | int | 报工数量 | NOT NULL |
| qualified_quantity | int | 合格数量 | |
| defective_quantity | int | 不合格数量 | DEFAULT 0 |
| report_time | datetime | 报工时间 | NOT NULL |
| sn_start | varchar(100) | 序列号起始 | |
| sn_end | varchar(100) | 序列号结束 | |
| remark | varchar(500) | 备注 | |
| create_time | datetime | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | datetime | 更新时间 | ON UPDATE CURRENT_TIMESTAMP |
| deleted | int | 逻辑删除 | DEFAULT 0 |
| deleted_time | datetime | 删除时间 | |
| deleted_by | bigint | 删除人ID | |
| version | int | 乐观锁 | DEFAULT 0 |

**索引**:
- `idx_work_order_id` (work_order_id)
- `idx_report_time` (report_time)

---

### 4. 工艺模板表 proc_template

| 字段 | 类型 | 说明 | 约束 |
|-----|------|------|------|
| id | bigint | 主键ID | PK, AUTO_INCREMENT |
| template_name | varchar(100) | 模板名称 | NOT NULL |
| template_code | varchar(50) | 模板编码 | NOT NULL, UNIQUE |
| product_model | varchar(100) | 适用产品型号 | |
| status | varchar(20) | 状态 | DEFAULT 'DRAFT' |
| version | int | 版本号 | DEFAULT 1 |
| remark | varchar(500) | 备注 | |
| create_by | bigint | 创建人ID | |
| publish_by | bigint | 发布人ID | |
| create_time | datetime | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | datetime | 更新时间 | ON UPDATE CURRENT_TIMESTAMP |
| deleted | int | 逻辑删除 | DEFAULT 0 |
| deleted_time | datetime | 删除时间 | |
| deleted_by | bigint | 删除人ID | |
| version | int | 乐观锁 | DEFAULT 0 |

**状态枚举**: DRAFT / PUBLISHED

---

### 5. 工艺参数表 proc_parameter

| 字段 | 类型 | 说明 | 约束 |
|-----|------|------|------|
| id | bigint | 主键ID | PK, AUTO_INCREMENT |
| template_id | bigint | 模板ID | NOT NULL |
| param_name | varchar(50) | 参数名称 | NOT NULL |
| param_code | varchar(50) | 参数编码 | NOT NULL |
| param_type | varchar(20) | 参数类型 | NOT NULL |
| param_value | varchar(100) | 参数值 | NOT NULL |
| unit | varchar(20) | 单位 | |
| min_value | double | 最小值 | |
| max_value | double | 最大值 | |
| create_time | datetime | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | datetime | 更新时间 | ON UPDATE CURRENT_TIMESTAMP |
| deleted | int | 逻辑删除 | DEFAULT 0 |
| deleted_time | datetime | 删除时间 | |
| deleted_by | bigint | 删除人ID | |
| version | int | 乐观锁 | DEFAULT 0 |

**索引**:
- `idx_template_id` (template_id)

---

### 6. 质检记录表 qms_quality_record

| 字段 | 类型 | 说明 | 约束 |
|-----|------|------|------|
| id | bigint | 主键ID | PK, AUTO_INCREMENT |
| work_order_id | bigint | 工单ID | |
| work_order_no | varchar(50) | 工单号 | |
| sn | varchar(100) | 产品序列号SN | |
| device_id | bigint | 设备ID | |
| workstation_id | bigint | 工位ID | |
| operator_id | bigint | 操作员ID | |
| check_type | varchar(20) | 检验类型 | |
| check_result | varchar(20) | 检验结果 | |
| defect_type | varchar(50) | 缺陷类型 | |
| defect_desc | varchar(500) | 缺陷描述 | |
| check_time | datetime | 检验时间 | |
| remark | varchar(500) | 备注 | |
| create_time | datetime | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | datetime | 更新时间 | ON UPDATE CURRENT_TIMESTAMP |
| deleted | int | 逻辑删除 | DEFAULT 0 |
| deleted_time | datetime | 删除时间 | |
| deleted_by | bigint | 删除人ID | |
| version | int | 乐观锁 | DEFAULT 0 |

**检验类型**: IPQC / FQC / OQC
**检验结果**: PASSED / FAILED / REWORK

**索引**:
- `idx_work_order_id` (work_order_id)
- `idx_sn` (sn)
- `idx_check_type` (check_type)
- `idx_check_result` (check_result)
- `idx_check_time` (check_time)

---

### 7. 追溯记录表 qms_traceability

| 字段 | 类型 | 说明 | 约束 |
|-----|------|------|------|
| id | bigint | 主键ID | PK, AUTO_INCREMENT |
| sn | varchar(100) | 产品序列号SN | NOT NULL |
| work_order_id | bigint | 工单ID | |
| work_order_no | varchar(50) | 工单号 | |
| product_name | varchar(100) | 产品名称 | |
| product_model | varchar(100) | 产品型号 | |
| process_name | varchar(100) | 工序名称 | |
| process_no | varchar(50) | 工序编号 | |
| device_id | bigint | 设备ID | |
| device_name | varchar(100) | 设备名称 | |
| operator_id | bigint | 操作员ID | |
| operator_name | varchar(50) | 操作员姓名 | |
| start_time | datetime | 开始时间 | |
| end_time | datetime | 结束时间 | |
| duration | int | 时长(秒) | |
| result | varchar(20) | 结果 | |
| parameters | text | 工艺参数(JSON) | |
| create_time | datetime | 创建时间 | DEFAULT CURRENT_TIMESTAMP |
| update_time | datetime | 更新时间 | ON UPDATE CURRENT_TIMESTAMP |
| deleted | int | 逻辑删除 | DEFAULT 0 |
| deleted_time | datetime | 删除时间 | |
| deleted_by | bigint | 删除人ID | |

**索引**:
- `idx_sn` (sn)
- `idx_work_order_id` (work_order_id)

---

## 表关系图

```
┌─────────────┐       ┌────────────────────┐       ┌─────────────────┐
│  sys_user  │       │  wo_work_order     │       │ proc_template   │
├─────────────┤       ├────────────────────┤       ├─────────────────┤
│ id (PK)    │◄──────│ create_by         │       │ id (PK)         │
│ username   │       │ issue_by          │◄──────│ process_template_id
│ password   │       │ work_order_id     │       │ (FK)           │
│ role       │       └────────────────────┘       └─────────────────┘
└─────────────┘                  │                        │
        │                      │                        │
        │              ┌───────▼────────┐     ┌───────▼────────┐
        │              │ wo_work_report │     │ proc_parameter│
        │              ├───────────────┤     ├──────────────┤
        └────────────►│ operator_id  │     │ template_id │
                    │ work_order_id│     │ (FK)       │
                    └─────────────┘     └─────────────┘
                           │
                           │
              ┌────────────▼────────────┐
              │ qms_quality_record   │
              ├─────────────────────┤
              │ id (PK)            │
              │ work_order_id (FK) │
              │ sn                │
              │ check_type       │
              │ check_result    │
              └─────────────────┘
                           │
                           ▼
              ┌─────────────────────┐
              │ qms_traceability  │
              ├──────────────────┤
              │ sn (索引)       │
              │ work_order_id  │
              │ work_order_no │
              └──────────────────┘
```

---

## 索引汇总

| 表名 | 索引名 | 字段 |
|-----|-------|------|
| sys_user | uk_username | username |
| wo_work_order | uk_order_no | order_no |
| wo_work_order | idx_status | status |
| wo_work_order | idx_create_time | create_time |
| wo_work_report | idx_work_order_id | work_order_id |
| wo_work_report | idx_report_time | report_time |
| proc_template | uk_template_code | template_code |
| proc_parameter | idx_template_id | template_id |
| qms_quality_record | idx_work_order_id | work_order_id |
| qms_quality_record | idx_sn | sn |
| qms_quality_record | idx_check_type | check_type |
| qms_quality_record | idx_check_result | check_result |
| qms_quality_record | idx_check_time | check_time |
| qms_traceability | idx_sn | sn |
| qms_traceability | idx_work_order_id | work_order_id |

---

## 版本历史

| 版本 | 日期 | 说明 |
|-----|------|------|
| V1 | 初始创建 | 基础表结构 |
| V2 | 2026-04-06 | 添加 deleted_time, deleted_by 字段 |
| V3 | 2026-04-10 | 完善质量管理表结构和示例数据 |