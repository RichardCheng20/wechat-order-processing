ALTER TABLE orders
    ADD COLUMN printed_at DATETIME NULL COMMENT '配送单打印时间' AFTER updated_at;
