CREATE TABLE supplier_purchase_lines (
    id              BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id     BIGINT         NOT NULL,
    supplier_id     BIGINT         NOT NULL,
    product_id      BIGINT         NOT NULL,
    effective_date  DATE           NOT NULL COMMENT '收货日',
    purchase_price  DECIMAL(10, 2) NOT NULL,
    purchased_qty   DECIMAL(12, 2) NOT NULL DEFAULT 0,
    deleted         TINYINT        NOT NULL DEFAULT 0,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_supplier_product_date (merchant_id, supplier_id, product_id, effective_date),
    KEY idx_spl_supplier (merchant_id, supplier_id, effective_date),
    KEY idx_spl_product (merchant_id, product_id, effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='按供应商拆分的采购明细';
