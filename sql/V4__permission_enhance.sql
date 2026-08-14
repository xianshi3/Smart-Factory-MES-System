-- ========================================
-- V4: 权限管理功能增强
-- 日期: 2026-04-12
-- 说明: 为角色-权限关联表添加排序字段，添加 sys_user.role_id 字段等
-- 幂等: 列/索引已存在时自动跳过，可安全重复执行
-- ========================================

USE mes_db;

DROP PROCEDURE IF EXISTS mes_v4_add_column;
DROP PROCEDURE IF EXISTS mes_v4_add_index;

DELIMITER //
CREATE PROCEDURE mes_v4_add_column(
    IN tbl_name VARCHAR(64),
    IN col_name VARCHAR(64),
    IN col_def TEXT
)
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl_name AND COLUMN_NAME = col_name;
    IF col_count = 0 THEN
        SET @sql_text = CONCAT('ALTER TABLE ', tbl_name, ' ADD COLUMN ', col_name, ' ', col_def);
        PREPARE stmt FROM @sql_text;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[OK] Added ', col_name, ' to ', tbl_name) AS result;
    ELSE
        SELECT CONCAT('[SKIP] ', col_name, ' already exists in ', tbl_name) AS result;
    END IF;
END //

CREATE PROCEDURE mes_v4_add_index(
    IN tbl_name VARCHAR(64),
    IN idx_name VARCHAR(64),
    IN idx_def TEXT
)
BEGIN
    DECLARE idx_count INT DEFAULT 0;
    SELECT COUNT(*) INTO idx_count FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl_name AND INDEX_NAME = idx_name;
    IF idx_count = 0 THEN
        SET @sql_text = CONCAT('ALTER TABLE ', tbl_name, ' ADD INDEX ', idx_name, ' ', idx_def);
        PREPARE stmt FROM @sql_text;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[OK] Added index ', idx_name, ' to ', tbl_name) AS result;
    ELSE
        SELECT CONCAT('[SKIP] index ', idx_name, ' already exists in ', tbl_name) AS result;
    END IF;
END //
DELIMITER ;

-- 为角色-权限关联表添加排序字段
CALL mes_v4_add_column('sys_role_permission', 'sort', "INT DEFAULT 0 COMMENT '排序'");

-- 为用户表添加role_id字段 (关联sys_role表)
CALL mes_v4_add_column('sys_user', 'role_id', "BIGINT COMMENT '角色ID'");

-- 创建用户-角色关联索引
CALL mes_v4_add_index('sys_user', 'idx_role_id', "(role_id)");

-- 初始化Admin用户角色为1 (超级管理员)
UPDATE sys_user SET role_id = 1 WHERE username = 'admin' AND role_id IS NULL;

-- 为角色表添加sort字段用于排序
CALL mes_v4_add_column('sys_role', 'sort', "INT DEFAULT 0 COMMENT '排序'");

-- 初始化角色排序
UPDATE sys_role SET sort = 1 WHERE role_code = 'ADMIN' AND (sort IS NULL OR sort = 0);
UPDATE sys_role SET sort = 2 WHERE role_code = 'MANAGER' AND (sort IS NULL OR sort = 0);
UPDATE sys_role SET sort = 3 WHERE role_code = 'USER' AND (sort IS NULL OR sort = 0);
UPDATE sys_role SET sort = 4 WHERE role_code = 'QC' AND (sort IS NULL OR sort = 0);
UPDATE sys_role SET sort = 5 WHERE role_code = 'ENGINEER' AND (sort IS NULL OR sort = 0);

-- 为权限表添加sort字段
CALL mes_v4_add_column('sys_permission', 'sort', "INT DEFAULT 0 COMMENT '排序'");

-- 初始化权限排序（如果还没有的话）
UPDATE sys_permission SET sort = 1 WHERE permission_code = 'dashboard' AND (sort IS NULL OR sort = 0);
UPDATE sys_permission SET sort = 2 WHERE permission_code = 'workorder' AND (sort IS NULL OR sort = 0);
UPDATE sys_permission SET sort = 3 WHERE permission_code = 'process' AND (sort IS NULL OR sort = 0);
UPDATE sys_permission SET sort = 4 WHERE permission_code = 'quality' AND (sort IS NULL OR sort = 0);
UPDATE sys_permission SET sort = 5 WHERE permission_code = 'device' AND (sort IS NULL OR sort = 0);
UPDATE sys_permission SET sort = 6 WHERE permission_code = 'report' AND (sort IS NULL OR sort = 0);
UPDATE sys_permission SET sort = 7 WHERE permission_code = 'profile' AND (sort IS NULL OR sort = 0);
UPDATE sys_permission SET sort = 8 WHERE permission_code = 'settings' AND (sort IS NULL OR sort = 0);
UPDATE sys_permission SET sort = 9 WHERE permission_code = 'user:manage' AND (sort IS NULL OR sort = 0);
UPDATE sys_permission SET sort = 10 WHERE permission_code = 'role:manage' AND (sort IS NULL OR sort = 0);

DROP PROCEDURE IF EXISTS mes_v4_add_column;
DROP PROCEDURE IF EXISTS mes_v4_add_index;
