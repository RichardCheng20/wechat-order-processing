-- 采购明细改为物理删除，清理历史逻辑删除残留，避免唯一键冲突
DELETE FROM supplier_purchase_lines WHERE deleted = 1;
