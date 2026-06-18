ALTER TABLE payments
    ADD COLUMN voucher_urls VARCHAR(2000) NULL COMMENT '凭证图片 URL，逗号分隔' AFTER remark;
