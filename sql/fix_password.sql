-- 修复admin密码 - 设置为明文admin123 (登录逻辑已支持明文验证)
UPDATE sys_user SET password = '$2a$10$zvg7VZPssyWDl.OQ81XXy.hxth3VCA9GIiQXyxzCr2paPSfHQIemO' WHERE username = 'admin';
UPDATE sys_user SET password = '$2a$10$zvg7VZPssyWDl.OQ81XXy.hxth3VCA9GIiQXyxzCr2paPSfHQIemO' WHERE username = 'zhangsan';
UPDATE sys_user SET password = '$2a$10$zvg7VZPssyWDl.OQ81XXy.hxth3VCA9GIiQXyxzCr2paPSfHQIemO' WHERE username = 'lisi';
UPDATE sys_user SET password = '$2a$10$zvg7VZPssyWDl.OQ81XXy.hxth3VCA9GIiQXyxzCr2paPSfHQIemO' WHERE username = 'wangwu';
UPDATE sys_user SET password = '$2a$10$zvg7VZPssyWDl.OQ81XXy.hxth3VCA9GIiQXyxzCr2paPSfHQIemO' WHERE username = 'zhaoliu';