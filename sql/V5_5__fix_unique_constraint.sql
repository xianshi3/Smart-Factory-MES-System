-- ========================================
-- V5_5: 修复逻辑删除与唯一约束冲突
-- 执行时间: 2026-05-04（2026-08 重写为幂等 + 修正列名）
-- 说明: 将单列唯一索引升级为 (业务字段, deleted) 复合唯一索引，
--       支持软删除后重新使用相同编码。
-- 幂等: 复合索引已存在时自动跳过；列名按最新 schema 修正
--       （proc_parameter.param_name / wo_work_report.report_time）。
-- ========================================

USE mes_db;

DROP PROCEDURE IF EXISTS mes_v5_5_ensure_unique;

DELIMITER //
CREATE PROCEDURE mes_v5_5_ensure_unique(
    IN tbl_name VARCHAR(64),
    IN idx_name VARCHAR(64),
    IN idx_cols VARCHAR(255)
)
BEGIN
    DECLARE cur_cols VARCHAR(255);
    DECLARE exists_any INT DEFAULT 0;

    SELECT GROUP_CONCAT(s.COLUMN_NAME ORDER BY s.SEQ_IN_INDEX) INTO cur_cols
    FROM INFORMATION_SCHEMA.STATISTICS s
    WHERE s.TABLE_SCHEMA = DATABASE()
      AND s.TABLE_NAME = tbl_name
      AND s.INDEX_NAME = idx_name;

    IF cur_cols IS NOT NULL AND cur_cols = idx_cols THEN
        SELECT CONCAT('[SKIP] index ', idx_name, ' already correct on ', tbl_name, ' (', cur_cols, ')') AS result;
    ELSE
        IF cur_cols IS NOT NULL THEN
            SET @drop_sql = CONCAT('ALTER TABLE ', tbl_name, ' DROP INDEX ', idx_name);
            PREPARE stmt FROM @drop_sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            SELECT CONCAT('[OK] Dropped old index ', idx_name, ' (', cur_cols, ') on ', tbl_name) AS result;
        END IF;
        SET @add_sql = CONCAT('ALTER TABLE ', tbl_name, ' ADD UNIQUE INDEX ', idx_name, ' (', idx_cols, ')');
        PREPARE stmt FROM @add_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SELECT CONCAT('[OK] Added unique index ', idx_name, ' on ', tbl_name, ' (', idx_cols, ')') AS result;
    END IF;
END //
DELIMITER ;

-- 1. 工艺模板表
CALL mes_v5_5_ensure_unique('proc_template', 'uk_template_code', 'template_code, deleted');

-- 2. 工艺参数表（注意列名为 param_name）
CALL mes_v5_5_ensure_unique('proc_parameter', 'uk_template_parameter', 'template_id, param_name, deleted');

-- 3. 工单表
CALL mes_v5_5_ensure_unique('wo_work_order', 'uk_order_no', 'order_no, deleted');

-- 4. 报工记录表（注意列名为 report_time）
CALL mes_v5_5_ensure_unique('wo_work_report', 'uk_work_report', 'work_order_id, report_time, deleted');

-- 5. 设备状态表
CALL mes_v5_5_ensure_unique('dash_device_status', 'uk_device_code', 'device_code, deleted');

-- 6. 用户表
CALL mes_v5_5_ensure_unique('sys_user', 'uk_username', 'username, deleted');

DROP PROCEDURE IF EXISTS mes_v5_5_ensure_unique;

-- 确认修复
SHOW INDEX FROM proc_template WHERE Key_name = 'uk_template_code';
