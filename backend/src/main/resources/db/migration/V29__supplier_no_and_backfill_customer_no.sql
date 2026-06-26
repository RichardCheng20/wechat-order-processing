-- 补全历史客户编号（yyyymmdd + 3位序号）
UPDATE customers c
    INNER JOIN (
        SELECT id,
               CONCAT(
                       DATE_FORMAT(DATE(created_at), '%Y%m%d'),
                       LPAD(
                               ROW_NUMBER() OVER (PARTITION BY merchant_id, DATE(created_at) ORDER BY id),
                               3, '0')
               ) AS new_no
        FROM customers
        WHERE deleted = 0
          AND (customer_no IS NULL OR customer_no = '')
    ) t ON c.id = t.id
SET c.customer_no = t.new_no;

-- 同步客户日序列表
INSERT INTO customer_id_sequences (merchant_id, seq_date, last_seq)
SELECT merchant_id,
       STR_TO_DATE(LEFT(customer_no, 8), '%Y%m%d') AS seq_date,
       MAX(CAST(RIGHT(customer_no, 3) AS UNSIGNED)) AS last_seq
FROM customers
WHERE customer_no IS NOT NULL
  AND LENGTH(customer_no) = 11
  AND deleted = 0
GROUP BY merchant_id, STR_TO_DATE(LEFT(customer_no, 8), '%Y%m%d')
ON DUPLICATE KEY UPDATE last_seq = GREATEST(customer_id_sequences.last_seq, VALUES(last_seq));

-- 供应商业务编号
ALTER TABLE suppliers
    ADD COLUMN supplier_no VARCHAR(11) NULL COMMENT '业务编号 yyyymmdd001' AFTER merchant_id;

CREATE TABLE supplier_id_sequences (
    merchant_id BIGINT NOT NULL,
    seq_date    DATE   NOT NULL,
    last_seq    INT    NOT NULL DEFAULT 0 COMMENT '当日已分配的最大序号',
    PRIMARY KEY (merchant_id, seq_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商编号日序列表';

UPDATE suppliers s
    INNER JOIN (
        SELECT id,
               CONCAT(
                       DATE_FORMAT(DATE(created_at), '%Y%m%d'),
                       LPAD(
                               ROW_NUMBER() OVER (PARTITION BY merchant_id, DATE(created_at) ORDER BY id),
                               3, '0')
               ) AS new_no
        FROM suppliers
        WHERE deleted = 0
          AND (supplier_no IS NULL OR supplier_no = '')
    ) t ON s.id = t.id
SET s.supplier_no = t.new_no;

INSERT INTO supplier_id_sequences (merchant_id, seq_date, last_seq)
SELECT merchant_id,
       STR_TO_DATE(LEFT(supplier_no, 8), '%Y%m%d') AS seq_date,
       MAX(CAST(RIGHT(supplier_no, 3) AS UNSIGNED)) AS last_seq
FROM suppliers
WHERE supplier_no IS NOT NULL
  AND LENGTH(supplier_no) = 11
  AND deleted = 0
GROUP BY merchant_id, STR_TO_DATE(LEFT(supplier_no, 8), '%Y%m%d')
ON DUPLICATE KEY UPDATE last_seq = GREATEST(supplier_id_sequences.last_seq, VALUES(last_seq));

ALTER TABLE suppliers
    MODIFY COLUMN supplier_no VARCHAR(11) NOT NULL COMMENT '业务编号 yyyymmdd001';

ALTER TABLE suppliers
    ADD UNIQUE KEY uk_suppliers_merchant_supplier_no (merchant_id, supplier_no);
