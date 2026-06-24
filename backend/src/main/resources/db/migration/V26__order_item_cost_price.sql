ALTER TABLE order_items
    ADD COLUMN cost_price DECIMAL(10, 2) NULL COMMENT '代采成本单价（仅老板端）' AFTER deal_price;
