ALTER TABLE products
    ADD COLUMN stock_qty DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '现有库存' AFTER default_purchase_price;

CREATE TABLE product_purchase_prices (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id     BIGINT         NOT NULL,
    product_id      BIGINT         NOT NULL,
    purchase_price  DECIMAL(10, 2) NOT NULL,
    effective_date  DATE           NOT NULL COMMENT '采购/收货日期',
    status          TINYINT        NOT NULL DEFAULT 1,
    deleted         TINYINT        NOT NULL DEFAULT 0,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_purchase_date (merchant_id, product_id, effective_date),
    KEY idx_purchase_date (merchant_id, effective_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品按日采购价';
