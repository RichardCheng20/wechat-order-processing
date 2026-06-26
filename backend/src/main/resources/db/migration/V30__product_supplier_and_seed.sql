ALTER TABLE products
    ADD COLUMN supplier_id BIGINT NULL COMMENT '默认供应商' AFTER category_id,
    ADD KEY idx_products_supplier (supplier_id);

DELETE FROM purchase_payments WHERE merchant_id = 1;
DELETE FROM suppliers WHERE merchant_id = 1;

SET @row_num = 0;
SET @date_prefix = DATE_FORMAT(CURDATE(), '%Y%m%d');

INSERT INTO suppliers (merchant_id, supplier_no, name, contact_name, phone, remark, payable_amount, paid_amount, status, deleted)
SELECT
    p.merchant_id,
    CONCAT(@date_prefix, LPAD(@row_num := @row_num + 1, 3, '0')),
    CONCAT(p.name, '供应商'),
    CONCAT(LEFT(p.name, 8), '档口'),
    CONCAT('138', LPAD(MOD(p.id, 100000000), 8, '0')),
    CONCAT('模拟供应商：专供', p.name),
    0,
    0,
    1,
    0
FROM products p
WHERE p.merchant_id = 1
  AND p.deleted = 0
  AND p.name <> '【系统】自定义商品'
ORDER BY p.id;

UPDATE products p
    INNER JOIN suppliers s ON s.merchant_id = p.merchant_id
        AND s.name = CONCAT(p.name, '供应商')
        AND s.deleted = 0
SET p.supplier_id = s.id
WHERE p.merchant_id = 1
  AND p.deleted = 0
  AND p.name <> '【系统】自定义商品';

INSERT INTO supplier_id_sequences (merchant_id, seq_date, last_seq)
SELECT 1, CURDATE(), COUNT(*)
FROM suppliers
WHERE merchant_id = 1 AND deleted = 0
ON DUPLICATE KEY UPDATE last_seq = GREATEST(supplier_id_sequences.last_seq, VALUES(last_seq));
