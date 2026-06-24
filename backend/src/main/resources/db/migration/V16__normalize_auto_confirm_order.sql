-- 统一订单流程：所有客户下单均从「待确认」开始
UPDATE customers SET auto_confirm_order = 0 WHERE auto_confirm_order != 0;

UPDATE users SET status = 'ENABLED'
WHERE role = 'CUSTOMER' AND customer_id IS NOT NULL AND status = 'PENDING_BIND';
