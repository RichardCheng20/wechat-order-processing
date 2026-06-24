-- 基础价 customer_id 从 NULL 统一为 1，并加唯一索引

-- 若 NULL 基础价与 customer_id=1 同商品同日期重复，删除 NULL 行（保留已有 1）
DELETE p_null
FROM product_prices p_null
INNER JOIN product_prices p_one
    ON p_null.merchant_id = p_one.merchant_id
   AND p_null.product_id = p_one.product_id
   AND p_one.customer_id = 1
   AND p_null.effective_date = p_one.effective_date
   AND p_null.deleted = p_one.deleted
WHERE p_null.customer_id IS NULL;

UPDATE product_prices SET customer_id = 1 WHERE customer_id IS NULL;

-- 同键去重，保留 id 较大的一条
DELETE t1
FROM product_prices t1
INNER JOIN product_prices t2
    ON t1.merchant_id = t2.merchant_id
   AND t1.product_id = t2.product_id
   AND t1.customer_id = t2.customer_id
   AND t1.effective_date = t2.effective_date
   AND t1.deleted = t2.deleted
   AND t1.id < t2.id;

ALTER TABLE product_prices
    MODIFY COLUMN customer_id BIGINT NOT NULL DEFAULT 1 COMMENT '1=基础价，其它=客户专属价';

CREATE UNIQUE INDEX uk_product_prices_scope_date
    ON product_prices (merchant_id, product_id, customer_id, effective_date, deleted);
