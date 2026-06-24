-- 手动执行：将客户 id=1 改为 id=4（与 Flyway V19 相同）
-- product_prices.customer_id=1 为基础价，不修改

SET @from_id = 1;
SET @to_id = 4;
SET @temp_id = 999990004;

UPDATE users SET customer_id = @temp_id WHERE customer_id = @to_id;
UPDATE orders SET customer_id = @temp_id WHERE customer_id = @to_id;
UPDATE payments SET customer_id = @temp_id WHERE customer_id = @to_id;
UPDATE product_prices SET customer_id = @temp_id WHERE customer_id = @to_id;
UPDATE customers SET id = @temp_id WHERE id = @to_id;

UPDATE users SET customer_id = @to_id WHERE customer_id = @from_id;
UPDATE orders SET customer_id = @to_id WHERE customer_id = @from_id;
UPDATE payments SET customer_id = @to_id WHERE customer_id = @from_id;
UPDATE customers SET id = @to_id WHERE id = @from_id;

SELECT id, name FROM customers WHERE id IN (1, 4, 999990004);
