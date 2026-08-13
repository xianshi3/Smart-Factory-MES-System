-- =====================================================
-- Smart Factory MES System - APS Scheduling (高级排程)
-- Version: V11
-- Description: 企业级排产：班次/工作日历、工序级排产明细、
--              变更日志、产能参数
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 班次定义表
DROP TABLE IF EXISTS `mes_shift`;
CREATE TABLE `mes_shift` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `shift_code`  VARCHAR(20)  NOT NULL COMMENT '班次编码: DAY/NIGHT',
    `shift_name`  VARCHAR(50)  NOT NULL COMMENT '班次名称',
    `start_time`  TIME         NOT NULL COMMENT '开始时间',
    `end_time`    TIME         NOT NULL COMMENT '结束时间',
    `is_work`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否排产可用: 1是 0否',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_shift_code` (`shift_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='排产班次表';

INSERT INTO `mes_shift` (`shift_code`, `shift_name`, `start_time`, `end_time`, `is_work`) VALUES
('DAY',   '白班', '08:00:00', '18:00:00', 1),
('NIGHT', '夜班', '20:00:00', '06:00:00', 1);

-- 2. 工作日历表（每天一行，标记是否排产）
DROP TABLE IF EXISTS `mes_work_calendar`;
CREATE TABLE `mes_work_calendar` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `work_date`  DATE        NOT NULL COMMENT '日期',
    `is_workday` TINYINT     NOT NULL DEFAULT 1 COMMENT '是否工作日: 1是 0否',
    `remark`     VARCHAR(200) DEFAULT NULL COMMENT '备注(如节假日)',
    `create_time` DATETIME   DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_work_date` (`work_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='排产工作日历';

-- 3. 排产明细表（工序级）
DROP TABLE IF EXISTS `wo_schedule`;
CREATE TABLE `wo_schedule` (
    `id`              BIGINT      NOT NULL COMMENT '主键(雪花)',
    `work_order_id`   BIGINT      NOT NULL COMMENT '工单ID',
    `step_id`         BIGINT      DEFAULT NULL COMMENT '工序ID(proc_step)',
    `step_no`         INT         NOT NULL DEFAULT 1 COMMENT '工序序号',
    `step_name`       VARCHAR(100) DEFAULT NULL COMMENT '工序名称',
    `workstation_id`  BIGINT      NOT NULL COMMENT '设备ID',
    `duration_min`    INT         NOT NULL DEFAULT 60 COMMENT '计划工时(分钟)',
    `planned_start`   DATETIME    NOT NULL COMMENT '计划开始',
    `planned_end`     DATETIME    NOT NULL COMMENT '计划结束',
    `sort_order`      INT         NOT NULL DEFAULT 0 COMMENT '同设备内顺序',
    `status`          VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT '排产状态: PLANNED-已排产 FROZEN-已冻结 RELEASED-已下发 HOLD-挂起',
    `bottleneck`      TINYINT     NOT NULL DEFAULT 0 COMMENT '是否瓶颈工序',
    `operator_id`     BIGINT      DEFAULT NULL COMMENT '排产人',
    `create_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_work_order` (`work_order_id`),
    KEY `idx_workstation` (`workstation_id`),
    KEY `idx_plan_time` (`planned_start`, `planned_end`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='排产明细表(工序级)';

-- 4. 排产变更日志表
DROP TABLE IF EXISTS `wo_schedule_log`;
CREATE TABLE `wo_schedule_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `work_order_id` BIGINT       DEFAULT NULL COMMENT '工单ID',
    `schedule_id`   BIGINT       DEFAULT NULL COMMENT '排产明细ID',
    `action`        VARCHAR(30)  NOT NULL COMMENT '操作: AUTO_PLAN/REPLAN/MOVE/SWAP/RESIZE/FREEZE/UNFREEZE/RELEASE/HOLD/UNDO/ASSIGN/UNASSIGN',
    `action_desc`   VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
    `before_json`   TEXT         DEFAULT NULL COMMENT '操作前快照',
    `after_json`    TEXT         DEFAULT NULL COMMENT '操作后快照',
    `operator_id`   BIGINT       DEFAULT NULL COMMENT '操作人',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_work_order` (`work_order_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='排产变更日志';

-- 5. 设备增加产能参数
ALTER TABLE `mes_workstation`
    ADD COLUMN `capacity_per_hour` INT NOT NULL DEFAULT 100 COMMENT '每小时产能(件)' AFTER `status`,
    ADD COLUMN `is_bottleneck` TINYINT NOT NULL DEFAULT 0 COMMENT '是否瓶颈设备' AFTER `capacity_per_hour`;

-- 6. 工艺工序种子数据（5个模板各3-4道工序）
INSERT INTO `proc_step` (`template_id`, `step_no`, `step_name`, `step_desc`, `duration_min`, `sequence`, `deleted`) VALUES
(1, 1, 'CNC粗加工', '铝合金外壳粗铣加工', 180, 1, 0),
(1, 2, 'CNC精加工', '精密尺寸精铣加工', 240, 2, 0),
(1, 3, '表面处理', '喷砂阳极氧化前处理', 120, 3, 0),
(1, 4, '成品检验', 'CNC件尺寸检验', 60, 4, 0),
(2, 1, '下料', '原材料切割下料', 90, 1, 0),
(2, 2, 'CNC加工', '标准CNC铣削加工', 180, 2, 0),
(2, 3, '去毛刺', '去毛刺打磨', 60, 3, 0),
(3, 1, '预组装', '零件预装配', 120, 1, 0),
(3, 2, '整机装配', '整机组装作业', 240, 2, 0),
(3, 3, '功能测试', '整机功能测试', 90, 3, 0),
(3, 4, '包装入库', '清洁包装入库', 60, 4, 0),
(4, 1, '外观检测', '外观缺陷检测', 60, 1, 0),
(4, 2, '功能检测', '性能功能测试', 120, 2, 0),
(4, 3, '尺寸检测', '高精度尺寸测量', 90, 3, 0),
(5, 1, '喷装底漆', '底漆喷涂', 90, 1, 0),
(5, 2, '喷装面漆', '面漆喷涂', 120, 2, 0),
(5, 3, '烘干固化', '高温烘干固化', 180, 3, 0);

-- 7. 初始化工作日历（从今天起60个自然日,周末非工作日）
INSERT INTO `mes_work_calendar` (`work_date`, `is_workday`)
SELECT DATE_ADD(CURDATE(), INTERVAL n DAY),
       CASE WHEN DAYOFWEEK(DATE_ADD(CURDATE(), INTERVAL n DAY)) IN (1, 7) THEN 0 ELSE 1 END
FROM (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
      UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11
      UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17
      UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23
      UNION SELECT 24 UNION SELECT 25 UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29
      UNION SELECT 30 UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35
      UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40 UNION SELECT 41
      UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45 UNION SELECT 46 UNION SELECT 47
      UNION SELECT 48 UNION SELECT 49 UNION SELECT 50 UNION SELECT 51 UNION SELECT 52 UNION SELECT 53
      UNION SELECT 54 UNION SELECT 55 UNION SELECT 56 UNION SELECT 57 UNION SELECT 58 UNION SELECT 59) nums;

-- 8. 种子工单关联工艺模板（为工序拆分做准备）
UPDATE `wo_work_order` SET `process_template_id` = 1 WHERE `id` IN (1, 6);
UPDATE `wo_work_order` SET `process_template_id` = 2 WHERE `id` = 2;
UPDATE `wo_work_order` SET `process_template_id` = 3 WHERE `id` IN (3, 5);
UPDATE `wo_work_order` SET `process_template_id` = 4 WHERE `id` IN (4, 8);
UPDATE `wo_work_order` SET `process_template_id` = 5 WHERE `id` = 7;

SET FOREIGN_KEY_CHECKS = 1;