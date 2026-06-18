CREATE TABLE suppliers (
    id              BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id     BIGINT         NOT NULL DEFAULT 1,
    name            VARCHAR(100)   NOT NULL COMMENT '供应商名称',
    contact_name    VARCHAR(50)    NULL,
    phone           VARCHAR(20)    NULL,
    remark          VARCHAR(500)   NULL,
    payable_amount  DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '累计应付',
    paid_amount     DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '累计已付',
    status          TINYINT        NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    deleted         TINYINT        NOT NULL DEFAULT 0,
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_suppliers_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商档案';

CREATE TABLE purchase_payments (
    id               BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id      BIGINT         NOT NULL DEFAULT 1,
    supplier_id      BIGINT         NOT NULL,
    amount           DECIMAL(12, 2) NOT NULL,
    method           VARCHAR(20)    NOT NULL COMMENT 'CASH/WECHAT/BANK_TRANSFER/OTHER',
    paid_at          DATETIME       NOT NULL,
    operator_user_id BIGINT         NULL,
    remark           VARCHAR(200)   NULL,
    voucher_urls     VARCHAR(2000)  NULL COMMENT '凭证图片 URL，逗号分隔',
    deleted          TINYINT        NOT NULL DEFAULT 0,
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_purchase_payments_supplier (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购付款记录';
