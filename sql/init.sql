-- =====================================================
-- Smart Factory MES System Database Initialization
-- Version: 1.0
-- Author: AI Assistant
-- Description: Create all tables for MES system
-- =====================================================

-- Set default charset
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. Authentication Module (sys_user)
-- =====================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `password` varchar(255) NOT NULL COMMENT '密码',
    `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
    `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
    `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
    `status` int DEFAULT '1' COMMENT '状态: 1-启用 0-禁用',
    `role` varchar(20) DEFAULT 'USER' COMMENT '角色: ADMIN/USER',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除: 0-未删除 1-已删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Insert default admin user (password: admin123)
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `phone`, `email`, `status`, `role`)
VALUES ('admin', 'admin123', '系统管理员', '13800138000', 'admin@mes.com', 1, 'ADMIN');

-- =====================================================
-- 2. Work Order Module (wo_work_order, wo_work_report)
-- =====================================================
DROP TABLE IF EXISTS `wo_work_order`;
CREATE TABLE `wo_work_order` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no` varchar(50) NOT NULL COMMENT '工单编号',
    `product_name` varchar(100) NOT NULL COMMENT '产品名称',
    `product_model` varchar(100) DEFAULT NULL COMMENT '产品型号',
    `plan_quantity` int NOT NULL COMMENT '计划数量',
    `completed_quantity` int DEFAULT '0' COMMENT '已完成数量',
    `status` varchar(20) NOT NULL COMMENT '状态: CREATED/ISSUED/IN_PRODUCTION/PENDING_QC/COMPLETED/CLOSED',
    `workstation_id` bigint DEFAULT NULL COMMENT '工位ID',
    `process_template_id` bigint DEFAULT NULL COMMENT '工艺模板ID',
    `priority` varchar(20) DEFAULT 'MEDIUM' COMMENT '优先级: LOW/MEDIUM/HIGH',
    `planned_start_time` datetime DEFAULT NULL COMMENT '计划开始时间',
    `planned_end_time` datetime DEFAULT NULL COMMENT '计划结束时间',
    `actual_start_time` datetime DEFAULT NULL COMMENT '实际开始时间',
    `actual_end_time` datetime DEFAULT NULL COMMENT '实际结束时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
    `issue_by` bigint DEFAULT NULL COMMENT '下发人ID',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单表';

DROP TABLE IF EXISTS `wo_work_report`;
CREATE TABLE `wo_work_report` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `work_order_id` bigint NOT NULL COMMENT '工单ID',
    `device_id` bigint DEFAULT NULL COMMENT '设备ID',
    `operator_id` bigint DEFAULT NULL COMMENT '操作员ID',
    `report_quantity` int NOT NULL COMMENT '报工数量',
    `qualified_quantity` int DEFAULT NULL COMMENT '合格数量',
    `defective_quantity` int DEFAULT '0' COMMENT '不合格数量',
    `report_time` datetime NOT NULL COMMENT '报工时间',
    `sn_start` varchar(100) DEFAULT NULL COMMENT '序列号起始',
    `sn_end` varchar(100) DEFAULT NULL COMMENT '序列号结束',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_work_order_id` (`work_order_id`),
    KEY `idx_report_time` (`report_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报工记录表';

-- =====================================================
-- 3. Process Module (proc_template, proc_parameter)
-- =====================================================
DROP TABLE IF EXISTS `proc_template`;
CREATE TABLE `proc_template` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_name` varchar(100) NOT NULL COMMENT '模板名称',
    `template_code` varchar(50) NOT NULL COMMENT '模板编码',
    `product_model` varchar(100) DEFAULT NULL COMMENT '适用产品型号',
    `status` varchar(20) DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工艺模板表';

DROP TABLE IF EXISTS `proc_parameter`;
CREATE TABLE `proc_parameter` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_id` bigint NOT NULL COMMENT '模板ID',
    `param_name` varchar(100) NOT NULL COMMENT '参数名称',
    `param_code` varchar(50) NOT NULL COMMENT '参数编码',
    `param_value` varchar(100) DEFAULT NULL COMMENT '默认值',
    `min_value` double DEFAULT NULL COMMENT '最小值',
    `max_value` double DEFAULT NULL COMMENT '最大值',
    `unit` varchar(20) DEFAULT NULL COMMENT '单位',
    `sort_order` int DEFAULT '0' COMMENT '排序',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工艺参数表';

-- =====================================================
-- 4. Quality Module (qms_quality_record, qms_traceability)
-- =====================================================
DROP TABLE IF EXISTS `qms_quality_record`;
CREATE TABLE `qms_quality_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `work_order_id` bigint NOT NULL COMMENT '工单ID',
    `work_order_no` varchar(50) DEFAULT NULL COMMENT '工单编号',
    `sn` varchar(100) DEFAULT NULL COMMENT '序列号',
    `device_id` bigint DEFAULT NULL COMMENT '设备ID',
    `workstation_id` bigint DEFAULT NULL COMMENT '工位ID',
    `operator_id` bigint DEFAULT NULL COMMENT '操作员ID',
    `check_type` varchar(20) DEFAULT NULL COMMENT '质检类型: IPQC/FQC/OQC',
    `check_result` varchar(20) DEFAULT NULL COMMENT '质检结果: PASSED/FAILED/REWORK',
    `defect_type` varchar(50) DEFAULT NULL COMMENT '缺陷类型',
    `defect_desc` varchar(500) DEFAULT NULL COMMENT '缺陷描述',
    `check_time` datetime NOT NULL COMMENT '质检时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_work_order_id` (`work_order_id`),
    KEY `idx_sn` (`sn`),
    KEY `idx_check_time` (`check_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='质检记录表';

DROP TABLE IF EXISTS `qms_traceability`;
CREATE TABLE `qms_traceability` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `sn` varchar(100) NOT NULL COMMENT '序列号',
    `work_order_id` bigint NOT NULL COMMENT '工单ID',
    `process_step` varchar(50) DEFAULT NULL COMMENT '工序步骤',
    `material_batch_no` varchar(50) DEFAULT NULL COMMENT '物料批次号',
    `equipment_id` bigint DEFAULT NULL COMMENT '设备ID',
    `operator_id` bigint DEFAULT NULL COMMENT '操作员ID',
    `param_snapshot` text COMMENT '参数快照(JSON)',
    `quality_result` varchar(20) DEFAULT NULL COMMENT '质量结果',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_sn` (`sn`),
    KEY `idx_work_order_id` (`work_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='追溯数据表';

-- =====================================================
-- 5. Dashboard Module (dash_device_status, dash_production_stats, dash_oee_data)
-- =====================================================
DROP TABLE IF EXISTS `dash_device_status`;
CREATE TABLE `dash_device_status` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_code` varchar(50) NOT NULL COMMENT '设备编码',
    `device_name` varchar(100) DEFAULT NULL COMMENT '设备名称',
    `status` varchar(20) DEFAULT NULL COMMENT '状态: ONLINE/OFFLINE/MAINTENANCE/ALARM',
    `temperature` double DEFAULT NULL COMMENT '温度',
    `speed` double DEFAULT NULL COMMENT '速度',
    `last_heartbeat` datetime DEFAULT NULL COMMENT '最后心跳时间',
    `workstation_id` bigint DEFAULT NULL COMMENT '工位ID',
    `production_line_id` bigint DEFAULT NULL COMMENT '生产线ID',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_code` (`device_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备状态表';

DROP TABLE IF EXISTS `dash_production_stats`;
CREATE TABLE `dash_production_stats` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `stat_date` varchar(10) NOT NULL COMMENT '统计日期(yyyy-MM-dd)',
    `workstation_id` bigint DEFAULT NULL COMMENT '工位ID',
    `work_order_id` bigint DEFAULT NULL COMMENT '工单ID',
    `plan_quantity` int DEFAULT '0' COMMENT '计划数量',
    `completed_quantity` int DEFAULT '0' COMMENT '完成数量',
    `qualified_quantity` int DEFAULT '0' COMMENT '合格数量',
    `defective_quantity` int DEFAULT '0' COMMENT '不合格数量',
    `oee_rate` double DEFAULT NULL COMMENT 'OEE',
    `availability_rate` double DEFAULT NULL COMMENT '可用率',
    `performance_rate` double DEFAULT NULL COMMENT '性能率',
    `quality_rate` double DEFAULT NULL COMMENT '质量率',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产统计表';

DROP TABLE IF EXISTS `dash_oee_data`;
CREATE TABLE `dash_oee_data` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id` bigint DEFAULT NULL COMMENT '设备ID',
    `date_hour` varchar(13) NOT NULL COMMENT '小时(yyyy-MM-dd-HH)',
    `available_time` bigint DEFAULT '0' COMMENT '可用时间(毫秒)',
    `run_time` bigint DEFAULT '0' COMMENT '运行时间(毫秒)',
    `downtime` bigint DEFAULT '0' COMMENT '停机时间(毫秒)',
    `total_products` int DEFAULT '0' COMMENT '总产量',
    `good_products` int DEFAULT '0' COMMENT '良品数',
    `defective_products` int DEFAULT '0' COMMENT '不良数',
    `ideal_cycle_time` double DEFAULT NULL COMMENT '理想周期时间',
    `oee` double DEFAULT NULL COMMENT 'OEE',
    `availability` double DEFAULT NULL COMMENT '可用率',
    `performance` double DEFAULT NULL COMMENT '性能率',
    `quality` double DEFAULT NULL COMMENT '质量率',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_device_date_hour` (`device_id`, `date_hour`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OEE数据表';

-- =====================================================
-- 6. Workstation and Production Line Tables
-- =====================================================
DROP TABLE IF EXISTS `mes_workstation`;
CREATE TABLE `mes_workstation` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `workstation_code` varchar(50) NOT NULL COMMENT '工位编码',
    `workstation_name` varchar(100) NOT NULL COMMENT '工位名称',
    `production_line_id` bigint DEFAULT NULL COMMENT '生产线ID',
    `status` varchar(20) DEFAULT 'IDLE' COMMENT '状态: IDLE/RUNNING/MAINTENANCE',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workstation_code` (`workstation_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工位表';

DROP TABLE IF EXISTS `mes_production_line`;
CREATE TABLE `mes_production_line` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `line_code` varchar(50) NOT NULL COMMENT '生产线编码',
    `line_name` varchar(100) NOT NULL COMMENT '生产线名称',
    `status` varchar(20) DEFAULT 'NORMAL' COMMENT '状态: NORMAL/MAINTENANCE/SHUTDOWN',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_line_code` (`line_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产线表';

-- Insert sample data
INSERT INTO `mes_production_line` (`line_code`, `line_name`, `status`) VALUES
('LINE-001', '总装生产线A', 'NORMAL'),
('LINE-002', '总装生产线B', 'NORMAL');

INSERT INTO `mes_workstation` (`workstation_code`, `workstation_name`, `production_line_id`, `status`) VALUES
('WS-001', '工位1', 1, 'IDLE'),
('WS-002', '工位2', 1, 'IDLE'),
('WS-003', '工位3', 2, 'IDLE'),
('WS-004', '工位4', 2, 'IDLE');

INSERT INTO `dash_device_status` (`device_code`, `device_name`, `status`, `workstation_id`) VALUES
('DEV-001', '设备1', 'ONLINE', 1),
('DEV-002', '设备2', 'ONLINE', 2),
('DEV-003', '设备3', 'ONLINE', 3),
('DEV-004', '设备4', 'OFFLINE', 4);

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- End of Initialization Script
-- =====================================================