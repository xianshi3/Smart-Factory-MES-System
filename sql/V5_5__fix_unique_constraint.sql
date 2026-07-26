-- 修复逻辑删除与唯一约束冲突
-- 执行时间: 2026-05-04
-- 问题: 使用 @TableLogic 软删除时，唯一约束冲突

-- 1. 工艺模板表
ALTER TABLE proc_template DROP INDEX uk_template_code;
ALTER TABLE proc_template ADD UNIQUE INDEX uk_template_code (template_code, deleted);

-- 2. 工艺参数表  
ALTER TABLE proc_parameter DROP INDEX uk_template_parameter;
ALTER TABLE proc_parameter ADD UNIQUE INDEX uk_template_parameter (template_id, parameter_name, deleted);

-- 3. 工单表
ALTER TABLE wo_work_order DROP INDEX uk_order_no;
ALTER TABLE wo_work_order ADD UNIQUE INDEX uk_order_no (order_no, deleted);

-- 4. 报工记录表
ALTER TABLE wo_work_report DROP INDEX uk_work_report;
ALTER TABLE wo_work_report ADD UNIQUE INDEX uk_work_report (work_order_id, work_time, deleted);

-- 5. 设备状态表
ALTER TABLE dash_device_status DROP INDEX uk_device_code;
ALTER TABLE dash_device_status ADD UNIQUE INDEX uk_device_code (device_code, deleted);

-- 6. 用户表
ALTER TABLE sys_user DROP INDEX uk_username;
ALTER TABLE sys_user ADD UNIQUE INDEX uk_username (username, deleted);

-- 确认修复
SHOW INDEX FROM proc_template WHERE Key_name = 'uk_template_code';