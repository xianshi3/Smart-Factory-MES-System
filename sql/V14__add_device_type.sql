-- =====================================================
-- V14__add_device_type.sql
-- 为 dash_device_status 表补 device_type 字段（幂等）
-- 服务器已手工加过列，可安全重复执行
-- =====================================================

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'dash_device_status'
      AND COLUMN_NAME = 'device_type'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `dash_device_status` ADD COLUMN `device_type` varchar(50) DEFAULT NULL COMMENT ''设备类型'' AFTER `device_name`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;