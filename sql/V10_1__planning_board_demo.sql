-- =====================================================
-- Smart Factory MES System - Planning Board demo data
-- Version: V10.1
-- Description: 将种子工单的计划时间调整到当前演示窗口，
--              以展示排产看板的各类状态（运行/延误/完成/待排产/已排产）
-- =====================================================

SET NAMES utf8mb4;

-- 将已分配设备的工单计划时间迁移到 2026-08 中旬窗口（按原顺序 12 小时间隔铺开）
SET @cursor = '2026-08-10 08:00:00';
UPDATE `wo_work_order` w
    JOIN (
        SELECT id,
               @cursor := DATE_ADD(@cursor, INTERVAL 12 HOUR) AS new_start,
               DATE_ADD(@cursor, INTERVAL 10 HOUR) AS new_end
        FROM (SELECT id, planned_start_time FROM wo_work_order
              WHERE deleted = 0 AND workstation_id IS NOT NULL
              ORDER BY planned_start_time ASC, id ASC) t
    ) s ON w.id = s.id
SET w.planned_start_time = s.new_start,
    w.planned_end_time   = s.new_end
WHERE w.deleted = 0 AND w.workstation_id IS NOT NULL;

-- 演示数据：为待排产工单补充计划时间（展示色块）
UPDATE `wo_work_order` SET
    planned_start_time = '2026-08-18 08:00:00',
    planned_end_time = '2026-08-18 18:00:00'
WHERE deleted = 0 AND workstation_id IS NULL;