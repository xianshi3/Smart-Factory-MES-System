ALTER TABLE proc_template DROP INDEX uk_template_code;
ALTER TABLE proc_template ADD UNIQUE KEY uk_template_code (template_code, deleted);
