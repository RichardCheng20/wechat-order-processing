-- 清空所有订单及关联数据（保留商品、客户、用户等主数据）
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM order_items;
DELETE FROM dispatch_logs;
DELETE FROM payments;
DELETE FROM purchase_payments;
DELETE FROM orders;
SET FOREIGN_KEY_CHECKS = 1;

-- 库存初始化：所有在售商品统一 100（可按需改数值）
UPDATE products
SET stock_qty = 100.00,
    updated_at = NOW()
WHERE deleted = 0;

SELECT 'orders' AS tbl, COUNT(*) AS cnt FROM orders
UNION ALL SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL SELECT 'payments', COUNT(*) FROM payments
UNION ALL SELECT 'dispatch_logs', COUNT(*) FROM dispatch_logs
UNION ALL SELECT 'purchase_payments', COUNT(*) FROM purchase_payments;

SELECT COUNT(*) AS product_count,
       ROUND(MIN(stock_qty), 2) AS min_stock,
       ROUND(MAX(stock_qty), 2) AS max_stock
FROM products
WHERE deleted = 0;
