-- 客户编号序列表 + 取消 customers.id 自增（改由应用按 yyyymmdd001 生成）

CREATE TABLE customer_id_sequences (
    merchant_id BIGINT NOT NULL,
    seq_date    DATE   NOT NULL,
    last_seq    INT    NOT NULL DEFAULT 0 COMMENT '当日已分配的最大序号',
    PRIMARY KEY (merchant_id, seq_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户编号日序列表';

ALTER TABLE customers
    MODIFY COLUMN id BIGINT NOT NULL COMMENT '客户编号：yyyymmdd+3位序号，如20260620001';
