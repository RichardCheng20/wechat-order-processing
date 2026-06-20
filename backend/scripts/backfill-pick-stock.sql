-- 补扣历史分拣出库未同步的库存（stock_applied_qty=0 且 actual_qty>0）
UPDATE products p
JOIN order_items oi ON oi.product_id = p.id AND oi.deleted = 0
SET p.stock_qty = GREATEST(p.stock_qty - (oi.actual_qty - oi.stock_applied_qty), 0),
    p.updated_at = NOW()
WHERE oi.stock_applied_qty < oi.actual_qty
  AND oi.actual_qty > 0
  AND oi.product_id IS NOT NULL;

UPDATE order_items
SET stock_applied_qty = actual_qty
WHERE stock_applied_qty < actual_qty
  AND actual_qty > 0;
