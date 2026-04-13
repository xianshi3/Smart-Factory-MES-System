-- Fix unique indexes to include deleted field for all tables

-- Process tables
ALTER TABLE proc_template DROP INDEX IF EXISTS uk_template_code;
ALTER TABLE proc_template ADD UNIQUE KEY uk_template_code (template_code, deleted);

ALTER TABLE proc_parameter DROP INDEX IF EXISTS uk_param_code;
ALTER TABLE proc_parameter ADD UNIQUE KEY uk_param_code (param_code, deleted);

-- Quality tables  
ALTER TABLE qms_quality_record MODIFY COLUMN work_order_id bigint DEFAULT NULL;
ALTER TABLE qms_quality_record MODIFY COLUMN work_order_no varchar(50) DEFAULT NULL;

ALTER TABLE qms_quality_record DROP INDEX IF EXISTS uk_sn_check_time;
ALTER TABLE qms_quality_record ADD UNIQUE KEY uk_sn_check_time (sn, check_time, deleted);

-- Work order tables  
ALTER TABLE wo_work_order DROP INDEX IF EXISTS uk_order_no;
ALTER TABLE wo_work_order ADD UNIQUE KEY uk_order_no (order_no, deleted);
