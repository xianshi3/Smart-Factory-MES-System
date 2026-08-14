-- =====================================================
-- V13__sync_permission_codes.sql
-- 补齐 sys_permission 权限码 + 角色分配，与 init.sql 定义对齐
-- 背景：旧库 sys_permission 仅有 13 条（缺 workorder:view / device:view / role:manage 等），
--       导致前端按权限码过滤时菜单/按钮被隐藏，普通角色授权无数据可依
-- 幂等：可重复执行
-- =====================================================

-- 1. 权限码（sys_permission.permission_code 唯一，INSERT IGNORE 防重）
INSERT IGNORE INTO `sys_permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `path`, `sort`) VALUES
('工单查看', 'workorder:view', 'BUTTON', 2, '', 1),
('工单创建', 'workorder:create', 'BUTTON', 2, '', 2),
('工单编辑', 'workorder:edit', 'BUTTON', 2, '', 3),
('工单删除', 'workorder:delete', 'BUTTON', 2, '', 4),
('工艺查看', 'process:view', 'BUTTON', 3, '', 1),
('工艺创建', 'process:create', 'BUTTON', 3, '', 2),
('工艺编辑', 'process:edit', 'BUTTON', 3, '', 3),
('质量查看', 'quality:view', 'BUTTON', 4, '', 1),
('质量创建', 'quality:create', 'BUTTON', 4, '', 2),
('质量删除', 'quality:delete', 'BUTTON', 4, '', 3),
('设备查看', 'device:view', 'BUTTON', 5, '', 1),
('设备控制', 'device:control', 'BUTTON', 5, '', 2),
('报表查看', 'report:view', 'BUTTON', 6, '', 1),
('报表导出', 'report:export', 'BUTTON', 6, '', 2),
('个人中心', 'profile', 'MENU', 0, '/profile', 7),
('系统设置', 'settings', 'MENU', 0, '/settings', 8),
('用户管理', 'user:manage', 'MENU', 0, '/user', 9),
('角色管理', 'role:manage', 'MENU', 0, '/role', 10),
('权限管理', 'permission:manage', 'MENU', 0, '/permission', 11);

-- 2. 角色分配（sys_role_permission 无唯一约束，用 NOT EXISTS 防重）
-- ADMIN(1)：拥有全部权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM sys_permission p
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

-- MANAGER(2)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, id FROM sys_permission p
WHERE p.permission_code IN ('dashboard', 'workorder', 'workorder:view', 'workorder:create', 'workorder:edit', 'process', 'process:view', 'quality', 'quality:view', 'device', 'device:view', 'report', 'report:view', 'profile')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 2 AND rp.permission_id = p.id);

-- USER(3)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 3, id FROM sys_permission p
WHERE p.permission_code IN ('dashboard', 'workorder', 'workorder:view', 'process', 'process:view', 'profile')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 3 AND rp.permission_id = p.id);

-- QC(4)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 4, id FROM sys_permission p
WHERE p.permission_code IN ('dashboard', 'quality', 'quality:view', 'quality:create', 'profile')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 4 AND rp.permission_id = p.id);

-- ENGINEER(5)
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 5, id FROM sys_permission p
WHERE p.permission_code IN ('dashboard', 'device', 'device:view', 'device:control', 'profile')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 5 AND rp.permission_id = p.id);