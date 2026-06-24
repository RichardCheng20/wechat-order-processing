-- 1. 基础价哨兵 customer_id: 1 → 0（V17 曾改为 1，现统一回 0）
UPDATE product_prices SET customer_id = 0 WHERE customer_id = 1;

ALTER TABLE product_prices
    MODIFY COLUMN customer_id BIGINT NOT NULL DEFAULT 0 COMMENT '0=基础价，其它=客户专属价';

-- 2. customer_no / uk_customers_merchant_customer_no 已在 V1 定义，此处仅确保 id 自增
ALTER TABLE customers
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '内部主键';
