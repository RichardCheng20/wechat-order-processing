ALTER TABLE product_purchase_prices
    ADD COLUMN purchased_qty DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '当日已采购数量' AFTER purchase_price;
