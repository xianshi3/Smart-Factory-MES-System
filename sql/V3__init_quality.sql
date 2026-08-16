-- =====================================================
-- Virtual Path MES - Quality Module Init
-- Version: 3.0
-- Date: 2026-04-10
-- Description: Initialize quality management tables
-- =====================================================

USE mes_db;

-- =====================================================
-- 质检记录表 qms_quality_record
-- =====================================================
DROP TABLE IF EXISTS `qms_quality_record`;
CREATE TABLE `qms_quality_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `work_order_id` bigint DEFAULT NULL COMMENT '工单ID',
    `work_order_no` varchar(50) DEFAULT NULL COMMENT '工单号',
    `sn` varchar(100) DEFAULT NULL COMMENT '产品序列号SN',
    `device_id` bigint DEFAULT NULL COMMENT '设备ID',
    `workstation_id` bigint DEFAULT NULL COMMENT '工位ID',
    `operator_id` bigint DEFAULT NULL COMMENT '操作员ID',
    `check_type` varchar(20) DEFAULT NULL COMMENT '检验类型: IPQC/FQC/OQC',
    `check_result` varchar(20) DEFAULT NULL COMMENT '检验结果: PASSED/FAILED/REWORK',
    `defect_type` varchar(50) DEFAULT NULL COMMENT '缺陷类型',
    `defect_desc` varchar(500) DEFAULT NULL COMMENT '缺陷描述',
    `check_time` datetime DEFAULT NULL COMMENT '检验时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除: 0-未删除 1-已删除',
    `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint DEFAULT NULL COMMENT '删除人ID',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_work_order_id` (`work_order_id`),
    KEY `idx_sn` (`sn`),
    KEY `idx_check_type` (`check_type`),
    KEY `idx_check_result` (`check_result`),
    KEY `idx_check_time` (`check_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质检记录表';

-- =====================================================
-- 追溯记录表 qms_traceability
-- =====================================================
DROP TABLE IF EXISTS `qms_traceability`;
CREATE TABLE `qms_traceability` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sn` varchar(100) NOT NULL COMMENT '产品序列号SN',
    `work_order_id` bigint DEFAULT NULL COMMENT '工单ID',
    `work_order_no` varchar(50) DEFAULT NULL COMMENT '工单号',
    `product_name` varchar(100) DEFAULT NULL COMMENT '产品名称',
    `product_model` varchar(100) DEFAULT NULL COMMENT '产品型号',
    `process_name` varchar(100) DEFAULT NULL COMMENT '工序名称',
    `process_no` varchar(50) DEFAULT NULL COMMENT '工序编号',
    `device_id` bigint DEFAULT NULL COMMENT '设备ID',
    `device_name` varchar(100) DEFAULT NULL COMMENT '设备名称',
    `operator_id` bigint DEFAULT NULL COMMENT '操作员ID',
    `operator_name` varchar(50) DEFAULT NULL COMMENT '操作员姓名',
    `start_time` datetime DEFAULT NULL COMMENT '开始时间',
    `end_time` datetime DEFAULT NULL COMMENT '结束时间',
    `duration` int DEFAULT NULL COMMENT '时长(秒)',
    `result` varchar(20) DEFAULT NULL COMMENT '结果: PASSED/FAILED',
    `parameters` text DEFAULT NULL COMMENT '工艺参数(JSON)',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint DEFAULT NULL COMMENT '删除人ID',
    PRIMARY KEY (`id`),
    KEY `idx_sn` (`sn`),
    KEY `idx_work_order_id` (`work_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='追溯记录表';

-- =====================================================
-- Insert sample quality records
-- =====================================================
INSERT INTO `qms_quality_record` 
(`work_order_id`, `work_order_no`, `sn`, `check_type`, `check_result`, `defect_type`, `check_time`) VALUES
(1, 'WO20260401001', 'SN00100001', 'IPQC', 'PASSED', NULL, NOW()),
(1, 'WO20260401001', 'SN00100002', 'IPQC', 'PASSED', NULL, NOW()),
(1, 'WO20260401001', 'SN00100003', 'IPQC', 'FAILED', '外观划痕', NOW()),
(1, 'WO20260401001', 'SN00100004', 'IPQC', 'PASSED', NULL, NOW()),
(1, 'WO20260401001', 'SN00100005', 'FQC', 'PASSED', NULL, NOW()),
(2, 'WO20260401002', 'SN00200001', 'IPQC', 'PASSED', NULL, NOW()),
(2, 'WO20260401002', 'SN00200002', 'IPQC', 'REWORK', '尺寸偏差', NOW());

-- =====================================================
-- Insert sample traceability records
-- =====================================================
INSERT INTO `qms_traceability` 
(`sn`, `work_order_id`, `work_order_no`, `product_name`, `process_name`, `process_no`, `result`, `start_time`, `end_time`) VALUES
('SN00100001', 1, 'WO20260401001', '产品A', '组装', 'PROC001', 'PASSED', NOW() - INTERVAL 1 HOUR, NOW()),
('SN00100002', 1, 'WO20260401001', '产品A', '组装', 'PROC001', 'PASSED', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 1 HOUR),
('SN00100003', 1, 'WO20260401001', '产品A', '测试', 'PROC002', 'FAILED', NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 2 HOUR),
('SN00100004', 1, 'WO20260401001', '产品A', '组装', 'PROC001', 'PASSED', NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 3 HOUR);