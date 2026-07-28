-- ============================================
-- V6: BOM 物料清单 + 库存管理
-- ============================================

-- 物料主数据
CREATE TABLE IF NOT EXISTS mes_db.material (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_code VARCHAR(50) NOT NULL COMMENT '物料编码',
    material_name VARCHAR(200) NOT NULL COMMENT '物料名称',
    material_type VARCHAR(50) NOT NULL COMMENT '物料类型: RAW/半成品/成品/辅助',
    unit VARCHAR(20) NOT NULL DEFAULT '个' COMMENT '单位',
    spec VARCHAR(200) COMMENT '规格型号',
    default_price DECIMAL(12,2) DEFAULT 0 COMMENT '默认单价',
    min_stock DECIMAL(12,2) DEFAULT 0 COMMENT '最低库存',
    max_stock DECIMAL(12,2) DEFAULT 0 COMMENT '最高库存',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    description TEXT,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_time DATETIME,
    deleted_by BIGINT,
    UNIQUE KEY uk_material_code (material_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料主数据';

-- BOM 物料清单
CREATE TABLE IF NOT EXISTS mes_db.bom (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bom_code VARCHAR(50) NOT NULL COMMENT 'BOM编号',
    bom_name VARCHAR(200) NOT NULL COMMENT 'BOM名称',
    product_id BIGINT NOT NULL COMMENT '成品物料ID',
    product_quantity DECIMAL(12,2) NOT NULL DEFAULT 1 COMMENT '成品数量',
    version VARCHAR(20) NOT NULL DEFAULT 'V1.0' COMMENT '版本',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/OBSOLETE',
    description TEXT,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    UNIQUE KEY uk_bom_code (bom_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM物料清单';

-- BOM 明细
CREATE TABLE IF NOT EXISTS mes_db.bom_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bom_id BIGINT NOT NULL COMMENT 'BOM ID',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    quantity DECIMAL(12,4) NOT NULL COMMENT '用量',
    unit VARCHAR(20) COMMENT '单位',
    scrap_rate DECIMAL(5,2) DEFAULT 0 COMMENT '损耗率(%)',
    sequence INT DEFAULT 0 COMMENT '工序顺序',
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_bom_id (bom_id),
    INDEX idx_material_id (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM明细';

-- 库存交易
CREATE TABLE IF NOT EXISTS mes_db.inventory_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_no VARCHAR(50) NOT NULL COMMENT '交易编号',
    material_id BIGINT NOT NULL COMMENT '物料ID',
    transaction_type VARCHAR(30) NOT NULL COMMENT '类型: RECEIVE/ISSUE/TRANSFER/ADJUST/RETURN',
    quantity DECIMAL(12,2) NOT NULL COMMENT '数量(正:入库,负:出库)',
    balance_after DECIMAL(12,2) NOT NULL COMMENT '交易后结存',
    batch_no VARCHAR(100) COMMENT '批次号',
    reference_type VARCHAR(50) COMMENT '关联类型: WORK_ORDER/BOM/QC',
    reference_id BIGINT COMMENT '关联ID',
    remark VARCHAR(500),
    create_by BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_material_id (material_id),
    INDEX idx_transaction_no (transaction_no),
    INDEX idx_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存交易记录';

-- 当前库存
CREATE TABLE IF NOT EXISTS mes_db.inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL COMMENT '物料ID',
    warehouse VARCHAR(50) DEFAULT '主仓库' COMMENT '仓库',
    batch_no VARCHAR(100) COMMENT '批次号',
    quantity DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '当前数量',
    locked_quantity DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '锁定数量',
    available_quantity DECIMAL(12,2) GENERATED ALWAYS AS (quantity - locked_quantity) STORED COMMENT '可用数量',
    last_transaction_time DATETIME,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_material_warehouse (material_id, warehouse, batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='当前库存';
