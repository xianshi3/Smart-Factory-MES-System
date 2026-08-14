-- ========================================
-- V2: 添加删除功能字段
-- 日期: 2026-04-06
-- 说明: 为所有业务表添加逻辑删除字段（deleted_time / deleted_by）
-- 幂等: init.sql 已包含这些列时自动跳过，可安全重复执行
-- ========================================

USE mes_db;

DROP PROCEDURE IF EXISTS mes_v2_add_column;

DELIMITER //
CREATE PROCEDURE mes_v2_add_column(
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
DELIMITER ;

CALL mes_v2_add_column('wo_work_order', 'deleted_time', "DATETIME COMMENT '删除时间'");
CALL mes_v2_add_column('wo_work_order', 'deleted_by',   "BIGINT COMMENT '删除人ID'");

CALL mes_v2_add_column('wo_work_report', 'deleted_time', "DATETIME COMMENT '删除时间'");
CALL mes_v2_add_column('wo_work_report', 'deleted_by',   "BIGINT COMMENT '删除人ID'");

CALL mes_v2_add_column('proc_template', 'deleted_time', "DATETIME COMMENT '删除时间'");
CALL mes_v2_add_column('proc_template', 'deleted_by',   "BIGINT COMMENT '删除人ID'");

CALL mes_v2_add_column('proc_parameter', 'deleted_time', "DATETIME COMMENT '删除时间'");
CALL mes_v2_add_column('proc_parameter', 'deleted_by',   "BIGINT COMMENT '删除人ID'");

CALL mes_v2_add_column('qms_quality_record', 'deleted_time', "DATETIME COMMENT '删除时间'");
CALL mes_v2_add_column('qms_quality_record', 'deleted_by',   "BIGINT COMMENT '删除人ID'");

CALL mes_v2_add_column('qms_traceability', 'deleted_time', "DATETIME COMMENT '删除时间'");
CALL mes_v2_add_column('qms_traceability', 'deleted_by',   "BIGINT COMMENT '删除人ID'");

CALL mes_v2_add_column('dash_device_status', 'deleted_time', "DATETIME COMMENT '删除时间'");
CALL mes_v2_add_column('dash_device_status', 'deleted_by',   "BIGINT COMMENT '删除人ID'");

CALL mes_v2_add_column('dash_production_stats', 'deleted_time', "DATETIME COMMENT '删除时间'");
CALL mes_v2_add_column('dash_production_stats', 'deleted_by',   "BIGINT COMMENT '删除人ID'");

CALL mes_v2_add_column('dash_oee_data', 'deleted_time', "DATETIME COMMENT '删除时间'");
CALL mes_v2_add_column('dash_oee_data', 'deleted_by',   "BIGINT COMMENT '删除人ID'");

CALL mes_v2_add_column('mes_workstation', 'deleted_time', "DATETIME COMMENT '删除时间'");
CALL mes_v2_add_column('mes_workstation', 'deleted_by',   "BIGINT COMMENT '删除人ID'");

CALL mes_v2_add_column('mes_production_line', 'deleted_time', "DATETIME COMMENT '删除时间'");
CALL mes_v2_add_column('mes_production_line', 'deleted_by',   "BIGINT COMMENT '删除人ID'");

DROP PROCEDURE IF EXISTS mes_v2_add_column;
