-- Reinitialize all menus with proper UTF-8 encoding
DELETE FROM sys_menu;

INSERT INTO sys_menu (menu_name, menu_code, parent_id, path, component, icon, sort, visible, status) VALUES
('首页', 'dashboard', 0, '/dashboard', 'dashboard/DashboardView', 'Odometer', 1, 1, 1),
('工单管理', 'workorder', 0, '/workorder', 'workorder/WorkOrderView', 'Document', 2, 1, 1),
('工艺管理', 'process', 0, '/process', 'process/ProcessView', 'Setting', 3, 1, 1),
('质量管理', 'quality', 0, '/quality', 'quality/QualityView', 'CircleCheck', 4, 1, 1),
('设备监控', 'device', 0, '/device', 'device/DeviceView', 'Monitor', 5, 1, 1),
('生产报表', 'report', 0, '/report', 'report/ReportView', 'DataAnalysis', 6, 1, 1),
('报警管理', 'alarm', 0, '/alarm', 'alarm/AlarmView', 'Warning', 7, 1, 1),
('角色管理', 'role', 0, '/role', 'role/RoleView', 'Key', 8, 1, 1);