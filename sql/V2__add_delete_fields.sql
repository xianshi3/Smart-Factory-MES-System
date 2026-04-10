-- ========================================
-- V2: 添加删除功能字段
-- 日期: 2026-04-06
-- 说明: 为所有业务表添加逻辑删除字段
-- init.sql 只包含 deleted，需添加 deleted_time 和 deleted_by
-- ========================================

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

-- 设备状态表 dash_device_status
ALTER TABLE dash_device_status 
ADD COLUMN deleted_time DATETIME COMMENT '删除时间',
ADD COLUMN deleted_by BIGINT COMMENT '删除人ID';

-- 生产统计表 dash_production_stats
ALTER TABLE dash_production_stats 
ADD COLUMN deleted_time DATETIME COMMENT '删除时间',
ADD COLUMN deleted_by BIGINT COMMENT '删除人ID';

-- OEE数据表 dash_oee_data
ALTER TABLE dash_oee_data 
ADD COLUMN deleted_time DATETIME COMMENT '删除时间',
ADD COLUMN deleted_by BIGINT COMMENT '删除人ID';

-- 工位表 mes_workstation
ALTER TABLE mes_workstation 
ADD COLUMN deleted_time DATETIME COMMENT '删除时间',
ADD COLUMN deleted_by BIGINT COMMENT '删除人ID';

-- 生产线表 mes_production_line
ALTER TABLE mes_production_line 
ADD COLUMN deleted_time DATETIME COMMENT '删除时间',
ADD COLUMN deleted_by BIGINT COMMENT '删除人ID';