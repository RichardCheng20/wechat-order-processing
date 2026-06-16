ALTER TABLE product_categories
    ADD COLUMN parent_id BIGINT NULL COMMENT '上级分类ID，空为一级分类' AFTER merchant_id;

ALTER TABLE product_categories
    ADD KEY idx_categories_parent (parent_id);
