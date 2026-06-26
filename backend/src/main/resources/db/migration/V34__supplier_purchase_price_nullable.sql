ALTER TABLE supplier_purchase_lines
    MODIFY COLUMN purchase_price DECIMAL(10, 2) NULL COMMENT '进价，未知时可空';
