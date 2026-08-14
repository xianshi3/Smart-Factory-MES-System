-- ========================================
-- V9: 数据库Schema修复与补齐
-- 日期: 2026-08-02
-- 说明: 补齐缺失的列/索引/种子数据，使DB与Java实体完全对齐
-- 用法: mysql -u root -proot --default-character-set=utf8mb4 < V9__schema_fix.sql
-- ========================================

USE mes_db;

DROP PROCEDURE IF EXISTS mes_add_column;

DELIMITER //
CREATE PROCEDURE mes_add_column(
    IN tbl_name VARCHAR(64),
    IN col_name VARCHAR(64),
    IN col_def TEXT
)
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'mes_db' AND TABLE_NAME = tbl_name AND COLUMN_NAME = col_name;
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
DELIMITER ;

-- =====================================================
-- 1. sys_user 补齐缺失列（与 User.java 实体对齐）
-- =====================================================
CALL mes_add_column('sys_user', 'nickname',    "VARCHAR(50) DEFAULT NULL COMMENT '昵称' AFTER real_name");
CALL mes_add_column('sys_user', 'avatar',      "VARCHAR(255) DEFAULT NULL COMMENT '头像URL' AFTER email");
CALL mes_add_column('sys_user', 'employee_no', "VARCHAR(50) DEFAULT NULL COMMENT '员工编号' AFTER avatar");
CALL mes_add_column('sys_user', 'department',  "VARCHAR(50) DEFAULT NULL COMMENT '部门' AFTER employee_no");
CALL mes_add_column('sys_user', 'position',    "VARCHAR(50) DEFAULT NULL COMMENT '岗位' AFTER department");
CALL mes_add_column('sys_user', 'manager_id',  "BIGINT DEFAULT NULL COMMENT '直接上级ID' AFTER position");
CALL mes_add_column('sys_user', 'hire_date',   "DATE DEFAULT NULL COMMENT '入职日期' AFTER manager_id");
CALL mes_add_column('sys_user', 'role_id',     "BIGINT DEFAULT NULL COMMENT '角色ID(关联sys_role)' AFTER role");

-- =====================================================
-- 2. sys_permission 补齐 icon 列
-- =====================================================
CALL mes_add_column('sys_permission', 'icon', "VARCHAR(50) DEFAULT NULL COMMENT '图标' AFTER path");

-- =====================================================
-- 3. sys_role_permission 补齐 sort 列
-- =====================================================
CALL mes_add_column('sys_role_permission', 'sort', "INT DEFAULT 0 COMMENT '排序' AFTER permission_id");

DROP PROCEDURE IF EXISTS mes_add_column;

-- =====================================================
-- 4. 修复 sys_user uk_username -> (username, deleted)
-- =====================================================
DROP PROCEDURE IF EXISTS mes_v9_fix_username_uk;

DELIMITER //
CREATE PROCEDURE mes_v9_fix_username_uk()
BEGIN
    DECLARE cur_cols VARCHAR(255);
    SELECT GROUP_CONCAT(s.COLUMN_NAME ORDER BY s.SEQ_IN_INDEX) INTO cur_cols
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = 'sys_user'
      AND s.INDEX_NAME = 'uk_username';
    IF cur_cols IS NULL OR cur_cols <> 'username, deleted' THEN
        IF cur_cols IS NOT NULL THEN
            ALTER TABLE sys_user DROP INDEX uk_username;
        END IF;
        ALTER TABLE sys_user ADD UNIQUE INDEX uk_username (username, deleted);
        SELECT '[OK] uk_username rebuilt to (username, deleted)' AS result;
    ELSE
        SELECT '[SKIP] uk_username already (username, deleted)' AS result;
    END IF;
END //
DELIMITER ;

CALL mes_v9_fix_username_uk();

DROP PROCEDURE IF EXISTS mes_v9_fix_username_uk;

-- =====================================================
-- 5. 更新已有用户数据
-- =====================================================
UPDATE sys_user SET
    nickname = '管理员',
    employee_no = 'EMP-001',
    department = '信息技术部',
    position = '系统管理员',
    hire_date = '2025-01-15',
    real_name = '张伟',
    role_id = 1
WHERE username = 'admin';

-- =====================================================
-- 6. 补充缺失的用户种子数据
-- =====================================================
INSERT IGNORE INTO sys_user (username, password, real_name, nickname, phone, email, employee_no, department, position, manager_id, hire_date, status, role, role_id)
VALUES
('zhangsan', '$2a$10$zvg7VZPssyWDl.OQ81XXy.hxth3VCA9GIiQXyxzCr2paPSfHQIemO', '张三', '张三', '13800138001', 'zhangsan@mes.com', 'EMP-002', '生产部', '生产主管', 1, '2025-03-20', 1, 'MANAGER', 2),
('lisi',     '$2a$10$zvg7VZPssyWDl.OQ81XXy.hxth3VCA9GIiQXyxzCr2paPSfHQIemO', '李四', '李四', '13800138002', 'lisi@mes.com', 'EMP-003', '生产部', '生产员工', 2, '2025-06-10', 1, 'USER', 3),
('wangwu',   '$2a$10$zvg7VZPssyWDl.OQ81XXy.hxth3VCA9GIiQXyxzCr2paPSfHQIemO', '王五', '王五', '13800138003', 'wangwu@mes.com', 'EMP-004', '质量管理部', '质检员', 2, '2025-07-01', 1, 'USER', 4),
('zhaoliu',  '$2a$10$zvg7VZPssyWDl.OQ81XXy.hxth3VCA9GIiQXyxzCr2paPSfHQIemO', '赵六', '赵六', '13800138004', 'zhaoliu@mes.com', 'EMP-005', '设备动力部', '设备工程师', 1, '2025-04-15', 1, 'USER', 5);

-- =====================================================
-- 7. 修复角色-权限关联排序
-- =====================================================
UPDATE sys_role_permission SET sort = 0 WHERE sort IS NULL;

-- =====================================================
-- 8. 密码说明
--    已移除"统一重置所有用户密码为 admin123"的语句（破坏性操作）。
--    新库用户密码由 init.sql 种子数据决定；如需重置请逐个 UPDATE。
-- =====================================================

-- =====================================================
-- 9. 补充报警菜单入口
-- =====================================================
INSERT IGNORE INTO sys_menu (menu_name, menu_code, parent_id, path, component, icon, sort)
VALUES ('报警管理', 'alarm', 0, '/alarm', 'alarm/AlarmView', 'Warning', 7);

-- =====================================================
-- 验证
-- =====================================================
SELECT id, username, real_name, nickname, department, position, role FROM sys_user WHERE deleted = 0;
