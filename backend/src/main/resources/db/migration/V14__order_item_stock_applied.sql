ALTER TABLE order_items
    ADD COLUMN stock_applied_qty DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '已从库存扣减的数量' AFTER actual_qty;
