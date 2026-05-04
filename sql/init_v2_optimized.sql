-- =====================================================
-- Smart Factory MES System Database Initialization
-- Version: 2.0 (Optimized)
-- Author: AI Assistant
-- Description: Optimized database schema with performance improvements
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. Authentication Module (sys_user) - OPTIMIZED
-- =====================================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `password` varchar(255) NOT NULL COMMENT '密码(BCrypt加密)',
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
    `role_id` bigint DEFAULT NULL COMMENT '角色ID(关联sys_role)',
    `role` varchar(20) DEFAULT 'USER' COMMENT '角色编码: ADMIN/USER/MANAGER',
    `status` tinyint DEFAULT '1' COMMENT '状态: 1-在职 0-离职',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除: 0-未删除 1-已删除',
    `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint DEFAULT NULL COMMENT '删除人ID',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_employee_no` (`employee_no`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_department` (`department`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =====================================================
-- 2. Role Module (sys_role) - OPTIMIZED
-- =====================================================
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_name` varchar(50) NOT NULL COMMENT '角色名称',
    `role_code` varchar(50) NOT NULL COMMENT '角色编码',
    `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
    `sort` int DEFAULT '0' COMMENT '排序(越小越靠前)',
    `status` tinyint DEFAULT '1' COMMENT '状态: 1-启用 0-禁用',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_status` (`status`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- =====================================================
-- 3. Permission Module (sys_permission) - OPTIMIZED
-- =====================================================
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `permission_name` varchar(100) NOT NULL COMMENT '权限名称',
    `permission_code` varchar(100) NOT NULL COMMENT '权限编码',
    `permission_type` varchar(20) NOT NULL COMMENT '权限类型: MENU/BUTTON/API',
    `parent_id` bigint DEFAULT '0' COMMENT '父权限ID',
    `path` varchar(255) DEFAULT NULL COMMENT '路由路径',
    `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
    `icon` varchar(50) DEFAULT NULL COMMENT '图标',
    `sort` int DEFAULT '0' COMMENT '排序',
    `status` tinyint DEFAULT '1' COMMENT '状态: 1-启用 0-禁用',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_permission_type` (`permission_type`),
    KEY `idx_status` (`status`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- =====================================================
-- 4. Role-Permission Relation (sys_role_permission) - OPTIMIZED
-- =====================================================
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `permission_id` bigint NOT NULL COMMENT '权限ID',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- =====================================================
-- 5. Menu Module (sys_menu) - OPTIMIZED
-- =====================================================
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
    `visible` tinyint DEFAULT '1' COMMENT '是否可见: 1-是 0-否',
    `status` tinyint DEFAULT '1' COMMENT '状态: 1-启用 0-禁用',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_menu_code` (`menu_code`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status` (`status`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单表';

-- Insert default data
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `nickname`, `phone`, `email`, `employee_no`, `department`, `position`, `role_id`, `role`, `status`) VALUES 
('admin', 'admin123', '张伟', '管理员', '13800138000', 'admin@mes.com', 'EMP-001', '信息技术部', '系统管理员', 1, 'ADMIN', 1),
('zhangsan', 'admin123', '张三', '张三', '13800138001', 'zhangsan@mes.com', 'EMP-002', '生产部', '生产主管', 2, 'MANAGER', 1),
('lisi', 'admin123', '李四', '李四', '13800138002', 'lisi@mes.com', 'EMP-003', '生产部', '生产员工', 3, 'USER', 1);

INSERT INTO `sys_role` (`role_name`, `role_code`, `description`, `sort`, `status`) VALUES
('超级管理员', 'ADMIN', '拥有系统所有权限', 1, 1),
('生产主管', 'MANAGER', '负责生产管理相关权限', 2, 1),
('生产员工', 'USER', '基本操作权限', 3, 1);

INSERT INTO `sys_permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `path`, `sort`) VALUES
('仪表盘', 'dashboard', 'MENU', 0, '/dashboard', 1),
('工单管理', 'workorder', 'MENU', 0, '/workorder', 2),
('工艺管理', 'process', 'MENU', 0, '/process', 3),
('质量管理', 'quality', 'MENU', 0, '/quality', 4),
('设备监控', 'device', 'MENU', 0, '/device', 5),
('生产报表', 'report', 'MENU', 0, '/report', 6),
('角色管理', 'role:manage', 'MENU', 0, '/role', 7);

INSERT INTO `sys_menu` (`menu_name`, `menu_code`, `parent_id`, `path`, `component`, `icon`, `sort`) VALUES
('仪表盘', 'dashboard', 0, '/dashboard', 'dashboard/DashboardView', 'Odometer', 1),
('工单管理', 'workorder', 0, '/workorder', 'workorder/WorkOrderView', 'Document', 2),
('工艺管理', 'process', 0, '/process', 'process/ProcessView', 'Setting', 3),
('质量管理', 'quality', 0, '/quality', 'quality/QualityView', 'CircleCheck', 4),
('设备监控', 'device', 0, '/device', 'device/DeviceView', 'Monitor', 5),
('生产报表', 'report', 0, '/report', 'report/ReportView', 'DataAnalysis', 6),
('报警管理', 'alarm', 0, '/alarm', 'alarm/AlarmView', 'Warning', 7),
('角色管理', 'role', 0, '/role', 'role/RoleView', 'Key', 8);

-- =====================================================
-- 6. Work Order Module (wo_work_order) - OPTIMIZED
-- =====================================================
DROP TABLE IF EXISTS `wo_work_order`;
CREATE TABLE `wo_work_order` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no` varchar(50) NOT NULL COMMENT '工单编号',
    `product_name` varchar(100) NOT NULL COMMENT '产品名称',
    `product_model` varchar(100) DEFAULT NULL COMMENT '产品型号',
    `plan_quantity` int NOT NULL COMMENT '计划数量',
    `completed_quantity` int DEFAULT '0' COMMENT '已完成数量',
    `status` varchar(20) NOT NULL COMMENT '工单状态',
    `workstation_id` bigint DEFAULT NULL COMMENT '工位ID',
    `process_template_id` bigint DEFAULT NULL COMMENT '工艺模板ID',
    `priority` varchar(20) DEFAULT 'MEDIUM' COMMENT '优先级',
    `planned_start_time` datetime DEFAULT NULL COMMENT '计划开始时间',
    `planned_end_time` datetime DEFAULT NULL COMMENT '计划结束时间',
    `actual_start_time` datetime DEFAULT NULL COMMENT '实际开始时间',
    `actual_end_time` datetime DEFAULT NULL COMMENT '实际结束时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
    `issue_by` bigint DEFAULT NULL COMMENT '下发人ID',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint DEFAULT NULL COMMENT '删除人ID',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_status` (`status`),
    KEY `idx_workstation_id` (`workstation_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_status_create` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单表';

-- =====================================================
-- 7. Work Report Module (wo_work_report) - OPTIMIZED
-- =====================================================
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
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint DEFAULT NULL COMMENT '删除人ID',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_work_order_id` (`work_order_id`),
    KEY `idx_device_id` (`device_id`),
    KEY `idx_operator_id` (`operator_id`),
    KEY `idx_report_time` (`report_time`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_wo_report_time` (`work_order_id`, `report_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报工记录表';

-- =====================================================
-- 8. Process Module - OPTIMIZED
-- =====================================================
DROP TABLE IF EXISTS `proc_template`;
CREATE TABLE `proc_template` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_name` varchar(100) NOT NULL COMMENT '模板名称',
    `template_code` varchar(50) NOT NULL COMMENT '模板编码',
    `product_model` varchar(100) DEFAULT NULL COMMENT '适用产品型号',
    `version` varchar(20) DEFAULT '1.0' COMMENT '版本号',
    `status` varchar(20) DEFAULT 'DRAFT' COMMENT '状态',
    `description` varchar(500) DEFAULT NULL COMMENT '描述',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint DEFAULT NULL COMMENT '删除人ID',
    `version_lock` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_code` (`template_code`, `deleted`),
    KEY `idx_status` (`status`),
    KEY `idx_product_model` (`product_model`),
    KEY `idx_deleted` (`deleted`)
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
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    `version_lock` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_template_sort` (`template_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工艺参数表';

-- =====================================================
-- 9. Quality Module - OPTIMIZED
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
    `check_type` varchar(20) DEFAULT NULL COMMENT '质检类型',
    `check_result` varchar(20) DEFAULT NULL COMMENT '质检结果',
    `defect_type` varchar(50) DEFAULT NULL COMMENT '缺陷类型',
    `defect_desc` varchar(500) DEFAULT NULL COMMENT '缺陷描述',
    `check_time` datetime NOT NULL COMMENT '质检时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint DEFAULT NULL COMMENT '删除人ID',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_work_order_id` (`work_order_id`),
    KEY `idx_sn` (`sn`),
    KEY `idx_check_time` (`check_time`),
    KEY `idx_check_result` (`check_result`),
    KEY `idx_wo_check_time` (`work_order_id`, `check_time`)
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
    `param_snapshot` json COMMENT '参数快照(JSON)',
    `quality_result` varchar(20) DEFAULT NULL COMMENT '质量结果',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sn` (`sn`),
    KEY `idx_work_order_id` (`work_order_id`),
    KEY `idx_process_step` (`process_step`),
    KEY `idx_quality_result` (`quality_result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='追溯数据表';

-- =====================================================
-- 10. Device Status Module - OPTIMIZED
-- =====================================================
DROP TABLE IF EXISTS `dash_device_status`;
CREATE TABLE `dash_device_status` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_code` varchar(50) NOT NULL COMMENT '设备编码',
    `device_name` varchar(100) DEFAULT NULL COMMENT '设备名称',
    `device_type` varchar(50) DEFAULT NULL COMMENT '设备类型',
    `status` varchar(20) DEFAULT NULL COMMENT '设备状态',
    `temperature` double DEFAULT NULL COMMENT '温度',
    `speed` double DEFAULT NULL COMMENT '速度',
    `last_heartbeat` datetime DEFAULT NULL COMMENT '最后心跳时间',
    `workstation_id` bigint DEFAULT NULL COMMENT '工位ID',
    `production_line_id` bigint DEFAULT NULL COMMENT '生产线ID',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_code` (`device_code`),
    KEY `idx_status` (`status`),
    KEY `idx_workstation_id` (`workstation_id`),
    KEY `idx_production_line_id` (`production_line_id`),
    KEY `idx_last_heartbeat` (`last_heartbeat`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备状态表';

-- =====================================================
-- 11. Production Statistics - OPTIMIZED
-- =====================================================
DROP TABLE IF EXISTS `dash_production_stats`;
CREATE TABLE `dash_production_stats` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `stat_date` date NOT NULL COMMENT '统计日期',
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
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_date_workstation` (`stat_date`, `workstation_id`),
    KEY `idx_stat_date` (`stat_date`),
    KEY `idx_workstation` (`workstation_id`),
    KEY `idx_work_order` (`work_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产统计表';

DROP TABLE IF EXISTS `dash_oee_data`;
CREATE TABLE `dash_oee_data` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id` bigint DEFAULT NULL COMMENT '设备ID',
    `stat_date` date NOT NULL COMMENT '统计日期',
    `stat_hour` tinyint NOT NULL COMMENT '统计小时(0-23)',
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
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_date_hour` (`device_id`, `stat_date`, `stat_hour`),
    KEY `idx_device_date` (`device_id`, `stat_date`),
    KEY `idx_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OEE数据表';

-- =====================================================
-- 12. Workstation and Production Line - OPTIMIZED
-- =====================================================
DROP TABLE IF EXISTS `mes_workstation`;
CREATE TABLE `mes_workstation` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `workstation_code` varchar(50) NOT NULL COMMENT '工位编码',
    `workstation_name` varchar(100) NOT NULL COMMENT '工位名称',
    `production_line_id` bigint DEFAULT NULL COMMENT '生产线ID',
    `status` varchar(20) DEFAULT 'IDLE' COMMENT '状态',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workstation_code` (`workstation_code`),
    KEY `idx_production_line_id` (`production_line_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工位表';

DROP TABLE IF EXISTS `mes_production_line`;
CREATE TABLE `mes_production_line` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `line_code` varchar(50) NOT NULL COMMENT '生产线编码',
    `line_name` varchar(100) NOT NULL COMMENT '生产线名称',
    `line_type` varchar(50) DEFAULT NULL COMMENT '生产线类型',
    `status` varchar(20) DEFAULT 'NORMAL' COMMENT '状态',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    `version` int DEFAULT '0' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_line_code` (`line_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='生产线表';

-- Insert sample data
INSERT INTO `mes_production_line` (`line_code`, `line_name`, `line_type`, `status`) VALUES
('LINE-001', '总装生产线A', '装配线', 'NORMAL'),
('LINE-002', '总装生产线B', '装配线', 'NORMAL');

INSERT INTO `mes_workstation` (`workstation_code`, `workstation_name`, `production_line_id`, `status`) VALUES
('WS-001', '工位1', 1, 'IDLE'),
('WS-002', '工位2', 1, 'IDLE'),
('WS-003', '工位3', 2, 'IDLE'),
('WS-004', '工位4', 2, 'IDLE');

SET FOREIGN_KEY_CHECKS = 1;
