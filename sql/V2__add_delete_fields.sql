-- ========================================
-- V2: 添加删除功能字段
-- 日期: 2026-04-06
-- 说明: 为缺少删除字段的表添加逻辑删除字段
-- 注意: 如果字段已存在会自动跳过
-- ========================================

-- 工单表 wo_work_order（检查 deleted 字段是否存在）
-- init.sql 已包含 deleted 字段，无需添加

-- 报工记录表 wo_work_report
-- init.sql 已包含 deleted 字段，无需添加

-- 工艺模板表 proc_template
-- init.sql 已包含 deleted 字段，无需添加

-- 工艺参数表 proc_parameter
-- init.sql 已包含 deleted 字段，无需添加

-- 质检记录表 qms_quality_record
-- init.sql 已包含 deleted 字段，无需添加

-- 追溯记录表 qms_traceability
-- init.sql 已包含 deleted 字段，无需添加

-- 设备状态表 dash_device_status（如有需要可添加）
-- 暂时跳过，等待后续需求

-- ========================================
-- 说明：由于 init.sql 已包含 deleted 字段
-- 此版本无需执行任何操作
-- 如需在已有数据库执行，请使用以下语句：
-- ========================================

-- 手动添加字段（如果 init.sql 没有包含）
-- ALTER TABLE table_name ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '0-未删除 1-已删除';
-- ALTER TABLE table_name ADD COLUMN deleted_time DATETIME COMMENT '删除时间';
-- ALTER TABLE table_name ADD COLUMN deleted_by BIGINT COMMENT '删除人ID';