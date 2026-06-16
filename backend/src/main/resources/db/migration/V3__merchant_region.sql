ALTER TABLE merchants
    ADD COLUMN region VARCHAR(100) NULL COMMENT '所在地区' AFTER phone;
