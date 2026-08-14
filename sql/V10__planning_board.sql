-- =====================================================
-- Smart Factory MES System - Planning Board (排产看板)
-- Version: V10
-- Description: 排产看板功能：工单增加设备内排产顺序字段
-- 幂等: init.sql 已包含 sort_order 时自动跳过
-- =====================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS mes_v10_add_sort_order;

DELIMITER //
CREATE PROCEDURE mes_v10_add_sort_order()
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'wo_work_order'
      AND COLUMN_NAME = 'sort_order';
    IF col_count = 0 THEN
        ALTER TABLE `wo_work_order`
            ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排产顺序(同设备内,0表示未排产)' AFTER `priority`;
        SELECT '[OK] Added wo_work_order.sort_order' AS result;
    ELSE
        SELECT '[SKIP] wo_work_order.sort_order already exists' AS result;
    END IF;
END //
DELIMITER ;

CALL mes_v10_add_sort_order();

DROP PROCEDURE IF EXISTS mes_v10_add_sort_order;

-- 初始化既有工单的排产顺序（按计划开始时间升序,同设备内编号；重复执行幂等）
UPDATE `wo_work_order` w
    JOIN (
        SELECT id, workstation_id,
               ROW_NUMBER() OVER (PARTITION BY workstation_id ORDER BY planned_start_time ASC, id ASC) AS rn
        FROM wo_work_order
        WHERE deleted = 0 AND workstation_id IS NOT NULL
    ) t ON w.id = t.id
SET w.sort_order = t.rn
WHERE w.workstation_id IS NOT NULL;
