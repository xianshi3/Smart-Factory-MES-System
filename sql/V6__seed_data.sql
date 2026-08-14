-- BOM + 物料测试数据
-- 注意：inventory.batch_no 固定为 'INIT-BATCH'，使 uk_material_warehouse 唯一键
--       (material_id, warehouse, batch_no) 在脚本重复执行时能正确去重（NULL 不参与唯一约束）
INSERT IGNORE INTO material (material_code, material_name, material_type, unit, spec, default_price, min_stock, max_stock, status) VALUES
('RAW-001', '铝合金板材 A6061', 'RAW', '张', '1200x800x2mm', 85.00, 50, 500, 'ACTIVE'),
('RAW-002', '不锈钢板 SUS304', 'RAW', '张', '1000x600x1.5mm', 120.00, 30, 300, 'ACTIVE'),
('RAW-003', 'PC塑料颗粒', 'RAW', 'kg', '透明阻燃级', 28.50, 100, 2000, 'ACTIVE'),
('RAW-004', '铜线缆 0.5mm²', 'RAW', 'm', '纯铜镀锡', 3.20, 500, 10000, 'ACTIVE'),
('RAW-005', '电子芯片 MCU-001', 'RAW', '个', 'ARM Cortex-M4', 15.00, 200, 5000, 'ACTIVE'),
('半成品-001', 'CNC加工毛坯', '半成品', '件', 'A6061粗加工', 45.00, 20, 200, 'ACTIVE'),
('半成品-002', '喷涂半成品', '半成品', '件', '表面处理完成', 35.00, 10, 100, 'ACTIVE'),
('成品-001', '智能手机外壳 A款', '成品', '个', 'A6061 阳极氧化', 180.00, 100, 2000, 'ACTIVE'),
('成品-002', '智能手机外壳 B款', '成品', '个', 'SUS304 PVD镀膜', 220.00, 80, 1500, 'ACTIVE'),
('辅助-001', '切削液', '辅助', '桶', '20L/桶', 180.00, 5, 30, 'ACTIVE'),
('辅助-002', '砂纸 800目', '辅助', '张', '800目耐磨', 1.50, 200, 5000, 'ACTIVE');

INSERT IGNORE INTO bom (bom_code, bom_name, product_id, product_quantity, version, status) VALUES
('BOM-2026-001', '智能手机外壳A款 BOM V1.0', 8, 1, 'V1.0', 'PUBLISHED');

INSERT IGNORE INTO bom_item (bom_id, material_id, quantity, unit, scrap_rate, sequence) VALUES
(1, 1, 0.5, '张', 3.00, 10),
(1, 4, 0.3, 'm', 1.00, 20),
(1, 5, 2, '个', 0.50, 30),
(1, 10, 0.02, '桶', 0.00, 40),
(1, 11, 2, '张', 5.00, 50);

INSERT IGNORE INTO inventory (material_id, warehouse, batch_no, quantity, locked_quantity) VALUES
(1, '主仓库', 'INIT-BATCH', 200, 0),
(2, '主仓库', 'INIT-BATCH', 150, 0),
(3, '主仓库', 'INIT-BATCH', 500, 0),
(4, '主仓库', 'INIT-BATCH', 3000, 0),
(5, '主仓库', 'INIT-BATCH', 2000, 0),
(6, '主仓库', 'INIT-BATCH', 50, 0),
(7, '主仓库', 'INIT-BATCH', 30, 0),
(8, '主仓库', 'INIT-BATCH', 500, 0),
(9, '主仓库', 'INIT-BATCH', 300, 0),
(10, '主仓库', 'INIT-BATCH', 15, 0),
(11, '主仓库', 'INIT-BATCH', 1000, 0);

-- 库存交易种子：用 NOT EXISTS 保证重复执行不产生重复流水
INSERT INTO inventory_transaction (transaction_no, material_id, transaction_type, quantity, balance_after, remark)
SELECT t.transaction_no, t.material_id, t.transaction_type, t.quantity, t.balance_after, t.remark
FROM (
    SELECT 'TX-20260727-0001' AS transaction_no, 1  AS material_id, 'RECEIVE' AS transaction_type, 200  AS quantity, 200  AS balance_after, '初始入库' AS remark
    UNION ALL SELECT 'TX-20260727-0002', 2,  'RECEIVE', 150,  150,  '初始入库'
    UNION ALL SELECT 'TX-20260727-0003', 3,  'RECEIVE', 500,  500,  '初始入库'
    UNION ALL SELECT 'TX-20260727-0004', 4,  'RECEIVE', 3000, 3000, '初始入库'
    UNION ALL SELECT 'TX-20260727-0005', 5,  'RECEIVE', 2000, 2000, '初始入库'
    UNION ALL SELECT 'TX-20260727-0006', 8,  'RECEIVE', 500,  500,  '初始入库'
    UNION ALL SELECT 'TX-20260727-0007', 9,  'RECEIVE', 300,  300,  '初始入库'
    UNION ALL SELECT 'TX-20260727-0008', 10, 'RECEIVE', 15,   15,   '初始入库'
) t
WHERE NOT EXISTS (SELECT 1 FROM inventory_transaction it WHERE it.transaction_no = t.transaction_no);
