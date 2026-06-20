ALTER TABLE orders
    ADD COLUMN statement_image_url VARCHAR(500) NULL COMMENT '对账单图片URL' AFTER printed_at;
