-- 模拟测试：重新生成售价，进价 = 售价 - 1 元
-- 用法：mysql ... vwholesale < backend/scripts/seed-mock-prices.sql

-- 1) 商品默认售价（约 5~20 元，保留 2 位小数）
UPDATE products
SET default_price = ROUND(5 + RAND() * 15, 2),
    updated_at    = NOW()
WHERE deleted = 0;

-- 2) 商品默认进价 = 售价 - 1
UPDATE products
SET default_purchase_price = GREATEST(ROUND(default_price - 1, 2), 0.01),
    updated_at             = NOW()
WHERE deleted = 0;

-- 3) 同步基础售价日表（customer_id = 0）
UPDATE product_prices pp
    INNER JOIN products p ON p.id = pp.product_id AND p.merchant_id = pp.merchant_id
SET pp.price       = p.default_price,
    pp.status      = 1,
    pp.deleted     = 0
WHERE pp.deleted = 0
  AND pp.customer_id = 0
  AND p.deleted = 0;

INSERT INTO product_prices (merchant_id, product_id, customer_id, price, effective_date, status, deleted, created_at)
SELECT p.merchant_id, p.id, 0, p.default_price, CURDATE(), 1, 0, NOW()
FROM products p
WHERE p.deleted = 0
  AND NOT EXISTS (SELECT 1
                  FROM product_prices pp
                  WHERE pp.product_id = p.id
                    AND pp.merchant_id = p.merchant_id
                    AND pp.customer_id = 0
                    AND pp.effective_date = CURDATE()
                    AND pp.deleted = 0);

-- 4) 今日进价日表 = 售价 - 1
INSERT INTO product_purchase_prices (merchant_id, product_id, purchase_price, purchased_qty, effective_date, status, deleted, created_at)
SELECT p.merchant_id,
       p.id,
       GREATEST(ROUND(p.default_price - 1, 2), 0.01),
       0,
       CURDATE(),
       1,
       0,
       NOW()
FROM products p
WHERE p.deleted = 0
ON DUPLICATE KEY UPDATE purchase_price = VALUES(purchase_price),
                        status         = 1,
                        deleted        = 0;

-- 5) 已有进价日表一并按新售价重算
UPDATE product_purchase_prices ppp
    INNER JOIN products p ON p.id = ppp.product_id AND p.merchant_id = ppp.merchant_id
SET ppp.purchase_price = GREATEST(ROUND(p.default_price - 1, 2), 0.01),
    ppp.status         = 1
WHERE ppp.deleted = 0
  AND p.deleted = 0;

SELECT COUNT(*)                                              AS product_count,
       ROUND(MIN(default_price), 2)                          AS min_sale,
       ROUND(MAX(default_price), 2)                          AS max_sale,
       ROUND(AVG(default_price), 2)                          AS avg_sale,
       ROUND(AVG(default_purchase_price), 2)                 AS avg_purchase,
       ROUND(AVG(default_price - default_purchase_price), 2) AS avg_margin
FROM products
WHERE deleted = 0;
