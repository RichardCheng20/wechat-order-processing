-- 将客户档案 id=1 调整为 id=4（幂等：客户 1 不存在时各语句影响 0 行）
-- 若 id=4 已被占用，请先执行 backend/scripts/migrate-customer-1-to-4.sql
-- product_prices.customer_id=1 保留为基础价桶，不在此修改

UPDATE users SET customer_id = 4 WHERE customer_id = 1;
UPDATE orders SET customer_id = 4 WHERE customer_id = 1;
UPDATE payments SET customer_id = 4 WHERE customer_id = 1;
UPDATE customers SET id = 4 WHERE id = 1;
