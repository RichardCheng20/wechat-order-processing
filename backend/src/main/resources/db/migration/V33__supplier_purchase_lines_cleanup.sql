-- 再次清理逻辑删除残留（幂等）
DELETE FROM supplier_purchase_lines WHERE deleted = 1;
