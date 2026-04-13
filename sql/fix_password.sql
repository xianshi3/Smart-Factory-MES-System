-- 修复admin密码 - 设置为明文admin123 (登录逻辑已支持明文验证)
UPDATE sys_user SET password = 'admin123' WHERE username = 'admin';
UPDATE sys_user SET password = 'admin123' WHERE username = 'zhangsan';
UPDATE sys_user SET password = 'admin123' WHERE username = 'lisi';
UPDATE sys_user SET password = 'admin123' WHERE username = 'wangwu';
UPDATE sys_user SET password = 'admin123' WHERE username = 'zhaoliu';