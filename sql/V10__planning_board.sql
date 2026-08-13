-- =====================================================
-- Smart Factory MES System - Planning Board (排产看板)
-- Version: V10
-- Description: 排产看板功能：工单增加设备内排产顺序字段
-- =====================================================

SET NAMES utf8mb4;

-- 排产看板：同设备内工单的排产先后顺序（拖拽调整时更新）
ALTER TABLE `wo_work_order`
    ADD COLUMN `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排产顺序(同设备内,0表示未排产)' AFTER `priority`;

-- 初始化既有工单的排产顺序（按计划开始时间升序,同设备内编号）
UPDATE `wo_work_order` w
    JOIN (
        SELECT id, workstation_id,
               ROW_NUMBER() OVER (PARTITION BY workstation_id ORDER BY planned_start_time ASC, id ASC) AS rn
        FROM wo_work_order
        WHERE deleted = 0 AND workstation_id IS NOT NULL
    ) t ON w.id = t.id
SET w.sort_order = t.rn
WHERE w.workstation_id IS NOT NULL;