-- 模拟测试：为所有上架商品生成随机库存（0~50，保留2位小数）
UPDATE products
SET stock_qty = ROUND(RAND() * 50, 2),
    updated_at  = NOW()
WHERE deleted = 0;

SELECT COUNT(*) AS updated_count,
       ROUND(MIN(stock_qty), 2) AS min_stock,
       ROUND(MAX(stock_qty), 2) AS max_stock,
       ROUND(AVG(stock_qty), 2) AS avg_stock
FROM products
WHERE deleted = 0;
