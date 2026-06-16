ALTER TABLE products
    ADD COLUMN sale_units VARCHAR(200) NULL COMMENT '可售单位，逗号分隔' AFTER unit;

UPDATE products SET sale_units = unit WHERE sale_units IS NULL OR sale_units = '';
