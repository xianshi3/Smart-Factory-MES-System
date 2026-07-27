
SET NAMES utf8mb4;
UPDATE material SET material_name='铝合金板材 A6061' WHERE material_code='RAW-001';
UPDATE material SET material_name='不锈钢板 SUS304' WHERE material_code='RAW-002';
UPDATE material SET material_name='PC塑料颗粒' WHERE material_code='RAW-003';
UPDATE material SET material_name='铜线缆 0.5mm2' WHERE material_code='RAW-004';
UPDATE material SET material_name='电子芯片 MCU-001' WHERE material_code='RAW-005';
UPDATE material SET material_name='CNC加工毛坯' WHERE material_code='CONG-001';
UPDATE material SET material_name='喷涂半成品' WHERE material_code='CONG-002';
UPDATE material SET material_name='智能手机外壳 A款' WHERE material_code='PROD-001';
UPDATE material SET material_name='智能手机外壳 B款' WHERE material_code='PROD-002';
UPDATE material SET material_name='切削液' WHERE material_code='AID-001';
UPDATE material SET material_name='砂纸 800目' WHERE material_code='AID-002';
SELECT 'OK' as result;