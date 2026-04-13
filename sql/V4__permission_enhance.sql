-- ========================================
-- V4: 权限管理功能增强
-- 日期: 2026-04-12
-- 说明: 为角色-权限关联表添加排序字段，添加sys_user表的role_id字段
-- ========================================

USE mes_db;

-- 为角色-权限关联表添加排序字段
ALTER TABLE sys_role_permission 
ADD COLUMN sort INT DEFAULT 0 COMMENT '排序';

-- 为用户表添加role_id字段 (关联sys_role表)
ALTER TABLE sys_user 
ADD COLUMN role_id BIGINT COMMENT '角色ID';

-- 创建用户-角色关联索引
ALTER TABLE sys_user ADD INDEX idx_role_id (role_id);

-- 初始化Admin用户角色为1 (超级管理员)
UPDATE sys_user SET role_id = 1 WHERE username = 'admin';

-- 为角色表添加sort字段用于排序
ALTER TABLE sys_role 
ADD COLUMN sort INT DEFAULT 0 COMMENT '排序';

-- 初始化角色排序
UPDATE sys_role SET sort = 1 WHERE role_code = 'ADMIN';
UPDATE sys_role SET sort = 2 WHERE role_code = 'MANAGER';
UPDATE sys_role SET sort = 3 WHERE role_code = 'USER';
UPDATE sys_role SET sort = 4 WHERE role_code = 'QC';
UPDATE sys_role SET sort = 5 WHERE role_code = 'ENGINEER';

-- 为权限表添加sort字段
ALTER TABLE sys_permission 
ADD COLUMN sort INT DEFAULT 0 COMMENT '排序';

-- 初始化权限排序（如果还没有的话）
UPDATE sys_permission SET sort = 1 WHERE permission_code = 'dashboard';
UPDATE sys_permission SET sort = 2 WHERE permission_code = 'workorder';
UPDATE sys_permission SET sort = 3 WHERE permission_code = 'process';
UPDATE sys_permission SET sort = 4 WHERE permission_code = 'quality';
UPDATE sys_permission SET sort = 5 WHERE permission_code = 'device';
UPDATE sys_permission SET sort = 6 WHERE permission_code = 'report';
UPDATE sys_permission SET sort = 7 WHERE permission_code = 'profile';
UPDATE sys_permission SET sort = 8 WHERE permission_code = 'settings';
UPDATE sys_permission SET sort = 9 WHERE permission_code = 'user:manage';
UPDATE sys_permission SET sort = 10 WHERE permission_code = 'role:manage';