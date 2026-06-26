ALTER TABLE merchants
    ADD COLUMN data_platform_password_hash VARCHAR(128) NULL COMMENT '数据平台查看密码哈希' AFTER owner_user_id;
