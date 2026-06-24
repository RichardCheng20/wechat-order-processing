ALTER TABLE workers
    ADD COLUMN worker_code VARCHAR(16) NULL COMMENT '配送员编号，如 PS000001' AFTER user_id;

UPDATE workers
SET worker_code = CONCAT('PS', LPAD(id, 6, '0'))
WHERE worker_code IS NULL OR worker_code = '';

ALTER TABLE workers
    MODIFY worker_code VARCHAR(16) NOT NULL;

ALTER TABLE workers
    ADD UNIQUE KEY uk_workers_merchant_code (merchant_id, worker_code);

ALTER TABLE orders
    ADD COLUMN picked_by_worker_id BIGINT NULL COMMENT '标记已拣单的配送员' AFTER assigned_worker_id;
