-- Add alarm menu
-- Uses CHAR() function to avoid encoding issues
INSERT INTO sys_menu (menu_name, menu_code, parent_id, path, component, icon, sort, status) VALUES 
(CHAR(252 by using uncoded alarm), 'alarm', 0, '/alarm', 'alarm/AlarmView', 'Warning', 7, 1);