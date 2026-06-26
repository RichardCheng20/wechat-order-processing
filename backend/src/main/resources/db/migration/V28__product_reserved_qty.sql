ALTER TABLE products
    ADD COLUMN reserved_qty DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '订单占用库存' AFTER stock_qty;

-- 待确认订单：补写明细占用量
UPDATE order_items oi
    INNER JOIN orders o ON o.id = oi.order_id AND o.deleted = 0
SET oi.stock_applied_qty = oi.order_qty
WHERE o.status = 'PENDING_CONFIRM'
  AND oi.deleted = 0
  AND oi.original_text IS NULL
  AND COALESCE(oi.stock_applied_qty, 0) = 0;

-- 待确认订单：汇总回填商品占用量
UPDATE products p
    INNER JOIN (
        SELECT oi.product_id, SUM(COALESCE(oi.stock_applied_qty, 0)) AS reserved_total
        FROM order_items oi
                 INNER JOIN orders o ON o.id = oi.order_id AND o.deleted = 0
        WHERE o.status = 'PENDING_CONFIRM'
          AND oi.deleted = 0
          AND oi.original_text IS NULL
        GROUP BY oi.product_id
    ) agg ON agg.product_id = p.id
SET p.reserved_qty = agg.reserved_total
WHERE p.deleted = 0;
