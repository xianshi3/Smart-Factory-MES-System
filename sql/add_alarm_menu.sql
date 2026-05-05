-- Add alarm menu for existing databases
INSERT INTO `sys_menu` (`menu_name`, `menu_code`, `parent_id`, `path`, `component`, `icon`, `sort`, `status`) VALUES
('报警管理', 'alarm', 0, '/alarm', 'alarm/AlarmView', 'Warning', 7, 1);