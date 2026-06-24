-- 示例商品与今日基础价
INSERT INTO products (merchant_id, category_id, name, aliases, unit, default_price, sale_status) VALUES
(1, 1, '上海青', '小青菜,青菜', '斤', 3.50, 'ON'),
(1, 1, '油麦菜', '油麦', '斤', 4.00, 'ON'),
(1, 1, '生菜', NULL, '斤', 3.80, 'ON'),
(1, 2, '土豆', '马铃薯', '斤', 2.20, 'ON'),
(1, 2, '胡萝卜', NULL, '斤', 2.80, 'ON'),
(1, 3, '西红柿', '番茄', '斤', 4.50, 'ON'),
(1, 3, '黄瓜', NULL, '斤', 3.60, 'ON'),
(1, 5, '大葱', NULL, '斤', 3.00, 'ON'),
(1, 6, '豆腐', NULL, '块', 2.50, 'ON'),
(1, 7, '金针菇', NULL, '包', 5.00, 'ON');

INSERT INTO product_prices (merchant_id, product_id, customer_id, price, effective_date, status)
SELECT 1, id, 0, default_price, CURDATE(), 1 FROM products WHERE merchant_id = 1;
