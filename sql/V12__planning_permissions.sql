-- =====================================================
-- V12__planning_permissions.sql
-- 排产看板权限：菜单 + 权限码(planning:view / planning:edit) + 角色分配
-- 幂等：可重复执行
-- =====================================================

-- 1. 菜单（sys_menu.menu_code 唯一）
INSERT INTO `sys_menu` (`menu_name`, `menu_code`, `parent_id`, `path`, `component`, `icon`, `sort`)
SELECT '生产调度', 'planning', 0, '/planning', 'schedule/PlanningBoardView', 'Calendar', 2
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'planning');

-- 2. 权限码（sys_permission.permission_code 唯一）
INSERT INTO `sys_permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `path`, `sort`)
SELECT '生产调度', 'planning', 'MENU', 0, '/planning', 2
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'planning');

INSERT INTO `sys_permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `path`, `sort`)
SELECT '排产查看', 'planning:view', 'BUTTON',
       (SELECT id FROM sys_permission WHERE permission_code = 'planning' LIMIT 1), '', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'planning:view');

INSERT INTO `sys_permission` (`permission_name`, `permission_code`, `permission_type`, `parent_id`, `path`, `sort`)
SELECT '排产编辑', 'planning:edit', 'BUTTON',
       (SELECT id FROM sys_permission WHERE permission_code = 'planning' LIMIT 1), '', 2
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'planning:edit');

-- 3. 角色分配（sys_role_permission 无唯一约束，用 NOT EXISTS 防重）
-- ADMIN(1)：全部排产权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM sys_permission
WHERE permission_code IN ('planning', 'planning:view', 'planning:edit')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = sys_permission.id);

-- MANAGER(2)：查看 + 编辑
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, id FROM sys_permission
WHERE permission_code IN ('planning', 'planning:view', 'planning:edit')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 2 AND rp.permission_id = sys_permission.id);

-- USER(3)：仅查看
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 3, id FROM sys_permission
WHERE permission_code IN ('planning', 'planning:view')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 3 AND rp.permission_id = sys_permission.id);