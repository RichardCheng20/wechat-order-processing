ALTER TABLE orders
    ADD COLUMN source_image_url VARCHAR(500) NULL COMMENT '客户下单原始图片URL' AFTER statement_image_url;
