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
    `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
    `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
    `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
    `employee_no` varchar(50) DEFAULT NULL COMMENT '员工编号',
    `department` varchar(50) DEFAULT NULL COMMENT '部门',
    `position` varchar(50) DEFAULT NULL COMMENT '岗位',
    `manager_id` bigint DEFAULT NULL COMMENT '直接上级ID',
    `hire_date` date DEFAULT NULL COMMENT '入职日期',
    `status` int DEFAULT '1' COMMENT '状态: 1-在职 0-离职',
    `role` varchar(20) DEFAULT 'USER' COMMENT '角色: ADMIN/USER/MANAGER',
    `role_id` bigint DEFAULT NULL COMMENT '角色ID(关联sys_role)',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除: 0-未删除 1-已删除',
    `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint DEFAULT NULL COMMENT '删除人ID',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`, `deleted`),
    UNIQUE KEY `uk_employee_no` (`employee_no`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Insert default admin user (password: admin123, BCrypt hashed)
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `nickname`, `phone`, `email`, `avatar`, `employee_no`, `department`, `position`, `manager_id`, `hire_date`, `status`, `role`, `role_id`)
VALUES 
('admin', '$2a$10$zvg7VZPssyWDl.OQ81XXy.hxth3VCA9GIiQXyxzCr2paPSfHQIemO', '张伟', '管理员', '13800138000', 'admin@mes.com', NULL, 'EMP-001', '信息技术部', '系统管理员', NULL, '2025-01-15', 1, 'ADMIN', 1),
('zhangsan', '$2a$10$zvg7VZPssyWDl.OQ81XXy.hxth3VCA9GIiQXyxzCr2paPSfHQIemO', '张三', '张三', '13800138001', 'zhangsan@mes.com', NULL, 'EMP-002', '生产部', '生产主管', NULL, '2025-03-20', 1, 'MANAGER', 2),
('lisi', '$2a$10$zvg7VZPssyWDl.OQ81XXy.hxth3VCA9GIiQXyxzCr2paPSfHQIemO', '李四', '李四', '13800138002', 'lisi@mes.com', NULL, 'EMP-003', '生产部', '生产员工', 2, '2025-06-10', 1, 'USER', 3),
('wangwu', '$2a$10$zvg7VZPssyWDl.OQ81XXy.hxth3VCA9GIiQXyxzCr2paPSfHQIemO', '王五', '王五', '13800138003', 'wangwu@mes.com', NULL, 'EMP-004', '质量管理部', '质检员', 2, '2025-07-01', 1, 'USER', 4),
('zhaoliu', '$2a$10$zvg7VZPssyWDl.OQ81XXy.hxth3VCA9GIiQXyxzCr2paPSfHQIemO', '赵六', '赵六', '13800138004', 'zhaoliu@mes.com', NULL, 'EMP-005', '设备动力部', '设备工程师', NULL, '2025-04-15', 1, 'USER', 5);

-- Update manager relationship
UPDATE sys_user SET manager_id = 1 WHERE username = 'zhangsan';
UPDATE sys_user SET manager_id = 2 WHERE username IN ('lisi', 'wangwu', 'zhaoliu');

-- =====================================================
-- 权限管理模块 (sys_role, sys_permission, sys_menu)
-- =====================================================

-- 角色表
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_name` varchar(50) NOT NULL COMMENT '角色名称',
    `role_code` varchar(50) NOT NULL COMMENT '角色编码',
    `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
    `sort` int DEFAULT '0' COMMENT '排序',
    `status` int DEFAULT '1' COMMENT '状态: 1-启用 0-禁用',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint DEFAULT NULL COMMENT '删除人ID',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 权限表
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `permission_name` varchar(100) NOT NULL COMMENT '权限名称',
    `permission_code` varchar(100) NOT NULL COMMENT '权限编码',
    `permission_type` varchar(20) NOT NULL COMMENT '权限类型: MENU/BUTTON/API',
    `parent_id` bigint DEFAULT '0' COMMENT '父权限ID',
    `path` varchar(255) DEFAULT NULL COMMENT '路由路径',
    `icon` varchar(50) DEFAULT NULL COMMENT '图标',
    `sort` int DEFAULT '0' COMMENT '排序',
    `status` int DEFAULT '1' COMMENT '状态: 1-启用 0-禁用',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint DEFAULT NULL COMMENT '删除人ID',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 角色-权限关联表
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `permission_id` bigint NOT NULL COMMENT '权限ID',
    `sort` int DEFAULT '0' COMMENT '排序',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint DEFAULT NULL COMMENT '删除人ID',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- 菜单表 (用于前端路由)
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
    `menu_code` varchar(50) NOT NULL COMMENT '菜单编码',
    `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
    `path` varchar(255) DEFAULT NULL COMMENT '路由路径',
    `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
    `icon` varchar(50) DEFAULT NULL COMMENT '图标',
    `sort` int DEFAULT '0' COMMENT '排序',
    `visible` int DEFAULT '1' COMMENT '是否可见: 1-是 0-否',
    `status` int DEFAULT '1' COMMENT '状态: 1-启用 0-禁用',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint DEFAULT NULL COMMENT '删除人ID',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_menu_code` (`menu_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

-- 插入角色数据
INSERT INTO `sys_role` (`role_name`, `role_code`, `description`, `sort`, `status`) VALUES
('超级管理员', 'ADMIN', '拥有系统所有权限', 1, 1),
('生产主管', 'MANAGER', '负责生产管理相关权限', 2, 1),
('生产员工', 'USER', '基本操作权限', 3, 1),
('质检员', 'QC', '质量检验相关权限', 4, 1),
('设备工程师', 'ENGINEER', '设备维护相关权限', 5, 1);

-- 插入权限数据
INSERT INTO `sys_permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `path`, `sort`) VALUES
('仪表盘', 'dashboard', 'MENU', 0, '/dashboard', 1),
('工单管理', 'workorder', 'MENU', 0, '/workorder', 2),
('工单查看', 'workorder:view', 'BUTTON', 2, '', 1),
('工单创建', 'workorder:create', 'BUTTON', 2, '', 2),
('工单编辑', 'workorder:edit', 'BUTTON', 2, '', 3),
('工单删除', 'workorder:delete', 'BUTTON', 2, '', 4),
('工艺管理', 'process', 'MENU', 0, '/process', 3),
('工艺查看', 'process:view', 'BUTTON', 3, '', 1),
('工艺创建', 'process:create', 'BUTTON', 3, '', 2),
('工艺编辑', 'process:edit', 'BUTTON', 3, '', 3),
('质量管理', 'quality', 'MENU', 0, '/quality', 4),
('质量查看', 'quality:view', 'BUTTON', 4, '', 1),
('质量创建', 'quality:create', 'BUTTON', 4, '', 2),
('质量删除', 'quality:delete', 'BUTTON', 4, '', 3),
('设备监控', 'device', 'MENU', 0, '/device', 5),
('设备查看', 'device:view', 'BUTTON', 5, '', 1),
('设备控制', 'device:control', 'BUTTON', 5, '', 2),
('生产报表', 'report', 'MENU', 0, '/report', 6),
('报表查看', 'report:view', 'BUTTON', 6, '', 1),
('报表导出', 'report:export', 'BUTTON', 6, '', 2),
('个人中心', 'profile', 'MENU', 0, '/profile', 7),
('系统设置', 'settings', 'MENU', 0, '/settings', 8),
('用户管理', 'user:manage', 'MENU', 0, '/user', 9),
('角色管理', 'role:manage', 'MENU', 0, '/role', 10),
('权限管理', 'permission:manage', 'MENU', 0, '/permission', 11);

-- 插入菜单数据
INSERT INTO `sys_menu` (`menu_name`, `menu_code`, `parent_id`, `path`, `component`, `icon`, `sort`) VALUES
('仪表盘', 'dashboard', 0, '/dashboard', 'dashboard/DashboardView', 'Odometer', 1),
('工单管理', 'workorder', 0, '/workorder', 'workorder/WorkOrderView', 'Document', 2),
('工艺管理', 'process', 0, '/process', 'process/ProcessView', 'Setting', 3),
('质量管理', 'quality', 0, '/quality', 'quality/QualityView', 'CircleCheck', 4),
('设备监控', 'device', 0, '/device', 'device/DeviceView', 'Monitor', 5),
('生产报表', 'report', 0, '/report', 'report/ReportView', 'DataAnalysis', 6),
('报警管理', 'alarm', 0, '/alarm', 'alarm/AlarmView', 'Warning', 7),
('个人中心', 'profile', 0, '/profile', 'profile/ProfileView', 'User', 8),
('系统设置', 'settings', 0, '/settings', 'settings/SettingsView', 'Setting', 9);

-- 角色分配权限 (ADMIN拥有所有权限)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM sys_permission WHERE deleted = 0;

-- MANAGER 角色权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, id FROM sys_permission WHERE permission_code IN ('dashboard', 'workorder', 'workorder:view', 'workorder:create', 'workorder:edit', 'process', 'process:view', 'quality', 'quality:view', 'device', 'device:view', 'report', 'report:view', 'profile');

-- USER 角色权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 3, id FROM sys_permission WHERE permission_code IN ('dashboard', 'workorder', 'workorder:view', 'process', 'process:view', 'profile');

-- QC 角色权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 4, id FROM sys_permission WHERE permission_code IN ('dashboard', 'quality', 'quality:view', 'quality:create', 'profile');

-- ENGINEER 角色权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 5, id FROM sys_permission WHERE permission_code IN ('dashboard', 'device', 'device:view', 'device:control', 'profile');

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
    `sort_order` int NOT NULL DEFAULT '0' COMMENT '排产顺序(同设备内,0表示未排产)',
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

DROP TABLE IF EXISTS `proc_step`;
CREATE TABLE `proc_step` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_id` bigint NOT NULL COMMENT '模板ID',
    `step_no` int NOT NULL DEFAULT '1' COMMENT '工序序号',
    `step_name` varchar(100) NOT NULL COMMENT '工序名称',
    `step_desc` varchar(500) DEFAULT NULL COMMENT '工序描述',
    `duration_min` int DEFAULT NULL COMMENT '标准工时(分钟)',
    `sequence` int NOT NULL DEFAULT '1' COMMENT '执行顺序',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工艺工序步骤表';

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

DROP TABLE IF EXISTS `dash_alarm_event`;
CREATE TABLE `dash_alarm_event` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `alarm_code` varchar(64) DEFAULT NULL COMMENT '告警编码',
    `message` text COMMENT '告警消息',
    `level` varchar(32) DEFAULT 'WARNING' COMMENT '级别: CRITICAL/WARNING/INFO',
    `alarm_type` varchar(64) DEFAULT NULL COMMENT '告警类型',
    `device_code` varchar(64) DEFAULT NULL COMMENT '设备编码',
    `device_name` varchar(128) DEFAULT NULL COMMENT '设备名称',
    `status` varchar(32) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/ACKNOWLEDGED/RESOLVED',
    `occurrence_time` datetime DEFAULT NULL COMMENT '发生时间',
    `ack_time` datetime DEFAULT NULL COMMENT '确认时间',
    `ack_user` varchar(64) DEFAULT NULL COMMENT '确认人',
    `resolve_time` datetime DEFAULT NULL COMMENT '解决时间',
    `resolve_user` varchar(64) DEFAULT NULL COMMENT '解决人',
    `remarks` text COMMENT '备注',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除',
    `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint DEFAULT NULL COMMENT '删除人ID',
    PRIMARY KEY (`id`),
    KEY `idx_device_code` (`device_code`),
    KEY `idx_status` (`status`),
    KEY `idx_occurrence_time` (`occurrence_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警事件表';

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
    `capacity_per_hour` int NOT NULL DEFAULT '100' COMMENT '每小时产能(件)',
    `is_bottleneck` tinyint NOT NULL DEFAULT '0' COMMENT '是否瓶颈设备',
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

INSERT INTO `dash_device_status` (`device_code`, `device_name`, `status`, `temperature`, `speed`, `last_heartbeat`, `workstation_id`, `production_line_id`) VALUES
('DEV-001', 'CNC加工中心A1', 'ONLINE', 45.2, 1200, NOW(), 1, 1),
('DEV-002', 'CNC加工中心A2', 'ONLINE', 42.8, 1150, NOW(), 2, 1),
('DEV-003', 'CNC加工中心B1', 'ONLINE', 48.5, 1180, NOW(), 3, 2),
('DEV-004', 'CNC加工中心B2', 'OFFLINE', 25.0, 0, NOW(), 4, 2),
('DEV-005', '自动组装线A', 'ONLINE', 35.6, 850, NOW(), 1, 1),
('DEV-006', '自动组装线B', 'ONLINE', 38.2, 820, NOW(), 2, 1),
('DEV-007', '质量检测台A', 'ONLINE', 30.1, 500, NOW(), 3, 2),
('DEV-008', '质量检测台B', 'MAINTENANCE', 28.5, 0, NOW(), 4, 2),
('DEV-009', '阳极氧化线', 'ONLINE', 32.8, 600, NOW(), NULL, 1),
('DEV-010', '喷涂工作站', 'ONLINE', 40.5, 450, NOW(), NULL, 2),
('DEV-011', '激光刻蚀机', 'ALARM', 55.2, 0, NOW(), NULL, 1),
('DEV-012', '包装流水线', 'ONLINE', 28.3, 300, NOW(), NULL, 2);

-- =====================================================
-- 7. Sample Work Orders
-- =====================================================
INSERT INTO `wo_work_order` (`order_no`, `product_name`, `product_model`, `plan_quantity`, `completed_quantity`, `status`, `workstation_id`, `process_template_id`, `priority`, `planned_start_time`, `planned_end_time`, `actual_start_time`) VALUES
('WO-20260405001', '智能手机外壳', 'iPhone 16 Pro', 1000, 350, 'IN_PRODUCTION', 1, 1, 'HIGH', '2026-04-05 08:00:00', '2026-04-07 18:00:00', '2026-04-05 09:00:00'),
('WO-20260405002', '智能手表表壳', 'Apple Watch S10', 500, 0, 'ISSUED', 2, 2, 'MEDIUM', '2026-04-06 08:00:00', '2026-04-08 18:00:00', NULL),
('WO-20260405003', '平板电脑外壳', 'iPad Pro 13', 300, 300, 'COMPLETED', 3, 1, 'LOW', '2026-04-01 08:00:00', '2026-04-03 18:00:00', '2026-04-01 09:00:00'),
('WO-20260405004', '蓝牙耳机外壳', 'AirPods Pro 3', 2000, 800, 'IN_PRODUCTION', 4, 3, 'HIGH', '2026-04-04 08:00:00', '2026-04-06 18:00:00', '2026-04-04 09:30:00'),
('WO-20260405005', '笔记本电脑外壳', 'MacBook Pro 16', 200, 0, 'CREATED', NULL, 2, 'MEDIUM', '2026-04-08 08:00:00', '2026-04-12 18:00:00', NULL),
('WO-20260405006', '智能音箱外壳', 'HomePod mini', 1500, 1500, 'COMPLETED', 1, 3, 'LOW', '2026-03-28 08:00:00', '2026-04-01 18:00:00', '2026-03-28 09:00:00'),
('WO-20260405007', '无线充电器', 'MagSafe Charger', 3000, 1200, 'IN_PRODUCTION', 2, 1, 'HIGH', '2026-04-03 08:00:00', '2026-04-05 18:00:00', '2026-04-03 08:30:00'),
('WO-20260405008', 'VR头盔外壳', 'Vision Pro 2', 100, 0, 'PENDING_QC', 3, 2, 'HIGH', '2026-04-02 08:00:00', '2026-04-04 18:00:00', '2026-04-02 09:00:00');

-- =====================================================
-- 8. Sample Work Reports
-- =====================================================
INSERT INTO `wo_work_report` (`work_order_id`, `device_id`, `operator_id`, `report_quantity`, `qualified_quantity`, `defective_quantity`, `report_time`, `sn_start`, `sn_end`) VALUES
(1, 1, 1, 100, 98, 2, '2026-04-05 10:00:00', 'SN0010010001', 'SN0010010100'),
(1, 1, 1, 100, 97, 3, '2026-04-05 12:00:00', 'SN0010010101', 'SN0010010200'),
(1, 1, 1, 100, 99, 1, '2026-04-05 14:00:00', 'SN0010010201', 'SN0010010300'),
(1, 1, 1, 50, 50, 0, '2026-04-05 16:00:00', 'SN0010010301', 'SN0010010350'),
(4, 3, 1, 200, 195, 5, '2026-04-04 10:00:00', 'SN0040010001', 'SN0040010200'),
(4, 3, 1, 200, 198, 2, '2026-04-04 14:00:00', 'SN0040010201', 'SN0040010400'),
(4, 3, 1, 200, 197, 3, '2026-04-04 16:00:00', 'SN0040010401', 'SN0040010600'),
(4, 4, 1, 200, 199, 1, '2026-04-05 09:00:00', 'SN0040010601', 'SN0040010800'),
(7, 2, 1, 300, 295, 5, '2026-04-03 10:00:00', 'SN0070010001', 'SN0070010300'),
(7, 2, 1, 300, 298, 2, '2026-04-03 14:00:00', 'SN0070010301', 'SN0070010600'),
(7, 2, 1, 300, 296, 4, '2026-04-04 10:00:00', 'SN0070010601', 'SN0070010900'),
(7, 2, 1, 300, 299, 1, '2026-04-04 14:00:00', 'SN0070010901', 'SN0070011200');

-- =====================================================
-- 9. Sample Process Templates
-- =====================================================
INSERT INTO `proc_template` (`template_name`, `template_code`, `product_model`, `status`, `description`) VALUES
('CNC精密加工工艺', 'CNC-001', 'iPhone 16 Pro', 'PUBLISHED', '适用于手机外壳的CNC精密加工工艺，包含粗加工、精加工、抛光等工序'),
('CNC标准工艺', 'CNC-002', '通用', 'PUBLISHED', '标准CNC加工工艺，适用于各类电子产品外壳'),
('组装工艺标准', 'ASM-001', '通用', 'PUBLISHED', '标准组装工艺流程，包含预装、组装、检测、包装工序'),
('检测工艺标准', 'INS-001', '通用', 'PUBLISHED', '质量检测工艺标准，包含外观检测、功能检测、尺寸检测'),
('阳极氧化工艺', 'ANO-001', 'iPhone 16 Pro', 'PUBLISHED', '金属外壳阳极氧化处理工艺'),
('喷涂工艺标准', 'SPR-001', '通用', 'DRAFT', '标准喷涂工艺，适用于塑料和金属外壳');

-- =====================================================
-- 10. Sample Process Parameters
-- =====================================================
INSERT INTO `proc_parameter` (`template_id`, `param_name`, `param_code`, `min_value`, `max_value`, `param_value`, `unit`, `sort_order`) VALUES
(1, '主轴转速', 'spindle_speed', 8000, 18000, '12000', 'RPM', 1),
(1, '进给速度', 'feed_rate', 500, 3000, '1500', 'mm/min', 2),
(1, '切削深度', 'cut_depth', 0.1, 2.0, '0.5', 'mm', 3),
(1, '冷却液流量', 'coolant_flow', 5, 20, '10', 'L/min', 4),
(2, '主轴转速', 'spindle_speed', 6000, 15000, '10000', 'RPM', 1),
(2, '进给速度', 'feed_rate', 300, 2000, '1000', 'mm/min', 2),
(2, '切削深度', 'cut_depth', 0.1, 3.0, '1.0', 'mm', 3),
(3, '组装压力', 'assembly_pressure', 10, 100, '50', 'N', 1),
(3, '扭矩', 'torque', 0.5, 5.0, '2.0', 'Nm', 2),
(3, '温度', 'temperature', 20, 40, '25', '℃', 3);

-- =====================================================
-- 11. Sample Quality Records
-- =====================================================
INSERT INTO `qms_quality_record` (`work_order_id`, `sn`, `check_type`, `check_result`, `defect_type`, `defect_desc`, `check_time`) VALUES
(1, 'SN0010010098', '巡检', 'PASS', NULL, NULL, '2026-04-05 10:30:00'),
(1, 'SN0010010099', '巡检', 'PASS', NULL, NULL, '2026-04-05 10:35:00'),
(1, 'SN0010010100', '巡检', 'FAIL', '外观缺陷', '表面有划痕', '2026-04-05 10:40:00'),
(4, 'SN0040010050', '巡检', 'PASS', NULL, NULL, '2026-04-04 11:00:00'),
(4, 'SN0040010051', '巡检', 'PASS', NULL, NULL, '2026-04-04 11:05:00'),
(4, 'SN0040010052', '巡检', 'FAIL', '尺寸超差', '孔径偏小0.15mm', '2026-04-04 11:10:00'),
(7, 'SN0070010001', '首检', 'PASS', NULL, NULL, '2026-04-03 10:30:00'),
(7, 'SN0070010002', '巡检', 'PASS', NULL, NULL, '2026-04-03 11:00:00'),
(7, 'SN0070010050', '巡检', 'PASS', NULL, NULL, '2026-04-04 15:00:00'),
(4, 'SN0040010800', '巡检', 'FAIL', '外观缺陷', '边缘有微小磕碰', '2026-04-05 09:30:00');

-- =====================================================
-- 12. Sample Production Statistics (Last 7 days)
-- =====================================================
INSERT INTO `dash_production_stats` (`stat_date`, `workstation_id`, `plan_quantity`, `completed_quantity`, `qualified_quantity`, `defective_quantity`, `oee_rate`, `availability_rate`, `performance_rate`, `quality_rate`) VALUES
('2026-03-30', 1, 500, 480, 470, 10, 0.85, 0.90, 0.96, 0.98),
('2026-03-31', 1, 500, 495, 485, 10, 0.88, 0.92, 0.96, 0.98),
('2026-04-01', 1, 500, 500, 492, 8, 0.90, 0.95, 0.95, 0.98),
('2026-04-02', 1, 500, 420, 410, 10, 0.75, 0.80, 0.96, 0.98),
('2026-04-03', 1, 500, 490, 480, 10, 0.87, 0.92, 0.95, 0.98),
('2026-04-04', 1, 500, 505, 495, 10, 0.90, 0.95, 0.95, 0.98),
('2026-04-05', 1, 500, 350, 344, 6, 0.82, 0.88, 0.94, 0.98),
('2026-03-30', 2, 400, 380, 372, 8, 0.82, 0.88, 0.95, 0.98),
('2026-03-31', 2, 400, 395, 387, 8, 0.86, 0.91, 0.95, 0.98),
('2026-04-01', 2, 400, 400, 392, 8, 0.88, 0.93, 0.95, 0.98),
('2026-04-02', 2, 400, 350, 343, 7, 0.78, 0.85, 0.94, 0.98),
('2026-04-03', 2, 400, 395, 387, 8, 0.86, 0.91, 0.95, 0.98),
('2026-04-04', 2, 400, 400, 392, 8, 0.88, 0.93, 0.95, 0.98),
('2026-04-05', 2, 400, 300, 294, 6, 0.75, 0.80, 0.94, 0.98);

-- =====================================================
-- 13. Sample OEE Data (Last 24 hours)
-- =====================================================
INSERT INTO `dash_oee_data` (`device_id`, `date_hour`, `available_time`, `run_time`, `downtime`, `total_products`, `good_products`, `defective_products`, `ideal_cycle_time`, `oee`, `availability`, `performance`, `quality`) VALUES
(1, '2026-04-04-08', 3600000, 3240000, 360000, 108, 106, 2, 30000, 0.80, 0.90, 0.90, 0.98),
(1, '2026-04-04-09', 3600000, 3420000, 180000, 114, 112, 2, 30000, 0.88, 0.95, 0.95, 0.98),
(1, '2026-04-04-10', 3600000, 3600000, 0, 120, 118, 2, 30000, 0.98, 1.00, 1.00, 0.98),
(1, '2026-04-04-14', 3600000, 3240000, 360000, 108, 106, 2, 30000, 0.80, 0.90, 0.90, 0.98),
(1, '2026-04-04-15', 3600000, 3420000, 180000, 114, 112, 2, 30000, 0.88, 0.95, 0.95, 0.98),
(1, '2026-04-05-08', 3600000, 2880000, 720000, 96, 94, 2, 30000, 0.70, 0.80, 0.80, 0.98),
(1, '2026-04-05-09', 3600000, 3600000, 0, 120, 118, 2, 30000, 0.98, 1.00, 1.00, 0.98),
(2, '2026-04-04-08', 3600000, 3420000, 180000, 95, 93, 2, 35000, 0.85, 0.95, 0.90, 0.98),
(2, '2026-04-04-09', 3600000, 3240000, 360000, 90, 88, 2, 35000, 0.75, 0.90, 0.86, 0.98),
(2, '2026-04-04-10', 3600000, 3600000, 0, 103, 101, 2, 35000, 1.00, 1.00, 1.00, 0.98),
(2, '2026-04-05-08', 3600000, 3600000, 0, 103, 101, 2, 35000, 1.00, 1.00, 1.00, 0.98),
(2, '2026-04-05-09', 3600000, 3420000, 180000, 95, 93, 2, 35000, 0.85, 0.95, 0.90, 0.98),
(3, '2026-04-04-08', 3600000, 3600000, 0, 80, 78, 2, 45000, 1.00, 1.00, 1.00, 0.98),
(3, '2026-04-04-09', 3600000, 3240000, 360000, 72, 71, 1, 45000, 0.72, 0.90, 0.80, 0.99),
(3, '2026-04-05-08', 3600000, 2880000, 720000, 64, 63, 1, 45000, 0.64, 0.80, 0.80, 0.98);

-- =====================================================
-- 8. AI Chat History Tables (ai_chat)
-- =====================================================
DROP TABLE IF EXISTS `ai_chat_messages`;
DROP TABLE IF EXISTS `ai_chat_conversations`;

CREATE TABLE `ai_chat_conversations` (
    `id` varchar(36) NOT NULL COMMENT '对话ID (UUID)',
    `user_id` varchar(50) NOT NULL DEFAULT 'default' COMMENT '用户ID',
    `title` varchar(200) NOT NULL DEFAULT '新对话' COMMENT '对话标题（自动截取首条消息）',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `deleted` int DEFAULT '0' COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 对话记录表';

CREATE TABLE `ai_chat_messages` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `conversation_id` varchar(36) NOT NULL COMMENT '所属对话ID',
    `role` varchar(20) NOT NULL COMMENT '角色: user / assistant',
    `content` text NOT NULL COMMENT '消息内容 (Markdown)',
    `steps` json DEFAULT NULL COMMENT 'Agent 执行步骤 (JSON)',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 消息记录表';

-- =====================================================
-- 9. AI Analysis History Table
-- =====================================================
DROP TABLE IF EXISTS `ai_analysis_history`;

CREATE TABLE `ai_analysis_history` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` varchar(50) NOT NULL DEFAULT 'default' COMMENT '用户ID',
    `device_code` varchar(50) DEFAULT NULL COMMENT '设备编码',
    `device_name` varchar(100) DEFAULT NULL COMMENT '设备名称',
    `analysis_type` varchar(20) NOT NULL COMMENT '分析类型: spc/energy/capacity/llm',
    `result_data` json DEFAULT NULL COMMENT '分析结果 (JSON)',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`analysis_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 分析历史记录表';

-- =====================================================
-- 10. BOM 物料清单 + 库存管理 Tables
-- =====================================================
DROP TABLE IF EXISTS `inventory`;
DROP TABLE IF EXISTS `inventory_transaction`;
DROP TABLE IF EXISTS `bom_item`;
DROP TABLE IF EXISTS `bom`;
DROP TABLE IF EXISTS `material`;

CREATE TABLE `material` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `material_code` varchar(50) NOT NULL COMMENT '物料编码',
    `material_name` varchar(200) NOT NULL COMMENT '物料名称',
    `material_type` varchar(50) NOT NULL COMMENT '物料类型: RAW/半成品/成品/辅助',
    `unit` varchar(20) NOT NULL DEFAULT '个' COMMENT '单位',
    `spec` varchar(200) DEFAULT NULL COMMENT '规格型号',
    `default_price` decimal(12,2) DEFAULT '0.00' COMMENT '默认单价',
    `min_stock` decimal(12,2) DEFAULT '0.00' COMMENT '最低库存',
    `max_stock` decimal(12,2) DEFAULT '0.00' COMMENT '最高库存',
    `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    `description` text COMMENT '物料描述',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0-未删除 1-已删除',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint DEFAULT NULL COMMENT '删除人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_material_code` (`material_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='物料主数据表';

CREATE TABLE `bom` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `bom_code` varchar(50) NOT NULL COMMENT 'BOM编号',
    `bom_name` varchar(200) NOT NULL COMMENT 'BOM名称',
    `product_id` bigint NOT NULL COMMENT '成品物料ID',
    `product_quantity` decimal(12,2) NOT NULL DEFAULT '1.00' COMMENT '成品数量',
    `version` varchar(20) NOT NULL DEFAULT 'V1.0' COMMENT '版本',
    `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/OBSOLETE',
    `description` text COMMENT 'BOM描述',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0-未删除 1-已删除',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
    `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bom_code` (`bom_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BOM物料清单表';

CREATE TABLE `bom_item` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `bom_id` bigint NOT NULL COMMENT 'BOM ID',
    `material_id` bigint NOT NULL COMMENT '物料ID',
    `quantity` decimal(12,4) NOT NULL COMMENT '用量',
    `unit` varchar(20) DEFAULT NULL COMMENT '单位',
    `scrap_rate` decimal(5,2) DEFAULT '0.00' COMMENT '损耗率(%)',
    `sequence` int DEFAULT '0' COMMENT '工序顺序',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0-未删除 1-已删除',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_bom_id` (`bom_id`),
    KEY `idx_material_id` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BOM明细表';

CREATE TABLE `inventory_transaction` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `transaction_no` varchar(50) NOT NULL COMMENT '交易编号',
    `material_id` bigint NOT NULL COMMENT '物料ID',
    `transaction_type` varchar(30) NOT NULL COMMENT '类型: RECEIVE/ISSUE/TRANSFER/ADJUST/RETURN',
    `quantity` decimal(12,2) NOT NULL COMMENT '数量(正:入库,负:出库)',
    `balance_after` decimal(12,2) NOT NULL COMMENT '交易后结存',
    `batch_no` varchar(100) DEFAULT NULL COMMENT '批次号',
    `reference_type` varchar(50) DEFAULT NULL COMMENT '关联类型: WORK_ORDER/BOM/QC',
    `reference_id` bigint DEFAULT NULL COMMENT '关联ID',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_material_id` (`material_id`),
    KEY `idx_transaction_no` (`transaction_no`),
    KEY `idx_batch_no` (`batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存交易记录表';

CREATE TABLE `inventory` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `material_id` bigint NOT NULL COMMENT '物料ID',
    `warehouse` varchar(50) DEFAULT '主仓库' COMMENT '仓库',
    `batch_no` varchar(100) DEFAULT NULL COMMENT '批次号',
    `quantity` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '当前数量',
    `locked_quantity` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '锁定数量',
    `available_quantity` decimal(12,2) GENERATED ALWAYS AS (quantity - locked_quantity) STORED COMMENT '可用数量',
    `last_transaction_time` datetime DEFAULT NULL COMMENT '最后交易时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_material_warehouse` (`material_id`, `warehouse`, `batch_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='当前库存表';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 8. Planning Module (排产看板 / APS 排程)
--    对应迁移: V10 (sort_order), V11 (班次/日历/排产明细/日志)
-- =====================================================

DROP TABLE IF EXISTS `mes_shift`;
CREATE TABLE `mes_shift` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `shift_code` varchar(20) NOT NULL COMMENT '班次编码: DAY/NIGHT',
    `shift_name` varchar(50) NOT NULL COMMENT '班次名称',
    `start_time` time NOT NULL COMMENT '开始时间',
    `end_time` time NOT NULL COMMENT '结束时间',
    `is_work` tinyint NOT NULL DEFAULT '1' COMMENT '是否排产可用: 1是 0否',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_shift_code` (`shift_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排产班次表';

INSERT INTO `mes_shift` (`shift_code`, `shift_name`, `start_time`, `end_time`, `is_work`) VALUES
('DAY', '白班', '08:00:00', '18:00:00', 1),
('NIGHT', '夜班', '20:00:00', '06:00:00', 1);

DROP TABLE IF EXISTS `mes_work_calendar`;
CREATE TABLE `mes_work_calendar` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `work_date` date NOT NULL COMMENT '日期',
    `is_workday` tinyint NOT NULL DEFAULT '1' COMMENT '是否工作日: 1是 0否',
    `remark` varchar(200) DEFAULT NULL COMMENT '备注(如节假日)',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_work_date` (`work_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排产工作日历';

DROP TABLE IF EXISTS `wo_schedule`;
CREATE TABLE `wo_schedule` (
    `id` bigint NOT NULL COMMENT '主键(雪花)',
    `work_order_id` bigint NOT NULL COMMENT '工单ID',
    `step_id` bigint DEFAULT NULL COMMENT '工序ID(proc_step)',
    `step_no` int NOT NULL DEFAULT '1' COMMENT '工序序号',
    `step_name` varchar(100) DEFAULT NULL COMMENT '工序名称',
    `workstation_id` bigint NOT NULL COMMENT '设备ID',
    `duration_min` int NOT NULL DEFAULT '60' COMMENT '计划工时(分钟)',
    `planned_start` datetime NOT NULL COMMENT '计划开始',
    `planned_end` datetime NOT NULL COMMENT '计划结束',
    `sort_order` int NOT NULL DEFAULT '0' COMMENT '同设备内顺序',
    `status` varchar(20) NOT NULL DEFAULT 'PLANNED' COMMENT '排产状态: PLANNED-已排产 FROZEN-已冻结 RELEASED-已下发 HOLD-挂起',
    `bottleneck` tinyint NOT NULL DEFAULT '0' COMMENT '是否瓶颈工序',
    `operator_id` bigint DEFAULT NULL COMMENT '排产人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` int NOT NULL DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_work_order` (`work_order_id`),
    KEY `idx_workstation` (`workstation_id`),
    KEY `idx_plan_time` (`planned_start`, `planned_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排产明细表(工序级)';

DROP TABLE IF EXISTS `wo_schedule_log`;
CREATE TABLE `wo_schedule_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `work_order_id` bigint DEFAULT NULL COMMENT '工单ID',
    `schedule_id` bigint DEFAULT NULL COMMENT '排产明细ID',
    `action` varchar(30) NOT NULL COMMENT '操作: AUTO_PLAN/REPLAN/MOVE/SWAP/RESIZE/FREEZE/UNFREEZE/RELEASE/HOLD/UNDO/ASSIGN/UNASSIGN',
    `action_desc` varchar(500) DEFAULT NULL COMMENT '操作描述',
    `before_json` text COMMENT '操作前快照',
    `after_json` text COMMENT '操作后快照',
    `operator_id` bigint DEFAULT NULL COMMENT '操作人',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_work_order` (`work_order_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排产变更日志';

-- =====================================================
-- End of Initialization Script
-- =====================================================