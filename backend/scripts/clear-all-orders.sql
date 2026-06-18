-- 清空所有订单及关联数据（保留商品、客户、用户等主数据）
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM order_items;
DELETE FROM dispatch_logs;
DELETE FROM payments;
DELETE FROM orders;
SET FOREIGN_KEY_CHECKS = 1;

SELECT 'orders' AS tbl, COUNT(*) AS cnt FROM orders
UNION ALL SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL SELECT 'payments', COUNT(*) FROM payments
UNION ALL SELECT 'dispatch_logs', COUNT(*) FROM dispatch_logs;
