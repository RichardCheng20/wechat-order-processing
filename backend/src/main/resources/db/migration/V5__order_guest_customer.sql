ALTER TABLE orders
    MODIFY COLUMN customer_id BIGINT NULL COMMENT '正式客户ID，临时开单可为空';

ALTER TABLE orders
    ADD COLUMN guest_customer_name VARCHAR(100) NULL COMMENT '临时客户名称（未建档）' AFTER customer_id;
