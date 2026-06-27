-- 档口级客户自助注册邀请 + 绑定申请审核

CREATE TABLE merchant_customer_register_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT       NOT NULL,
    token       VARCHAR(32)  NOT NULL COMMENT '注册邀请码',
    expired_at  DATETIME     NOT NULL,
    revoked_at  DATETIME     NULL COMMENT '重新生成时作废',
    created_by  BIGINT       NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_merchant_register_token (token),
    KEY idx_mcr_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='档口客户自助注册邀请码';

CREATE TABLE customer_bind_requests (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id   BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL COMMENT '申请人微信用户',
    shop_name     VARCHAR(128) NOT NULL,
    contact_name  VARCHAR(64)  NULL,
    phone         VARCHAR(32)  NULL,
    address       VARCHAR(512) NULL,
    address_short VARCHAR(128) NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    customer_id   BIGINT       NULL COMMENT '审核通过后关联客户档案',
    reject_reason VARCHAR(256) NULL,
    reviewed_by   BIGINT       NULL,
    reviewed_at   DATETIME     NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_cbr_merchant_status (merchant_id, status),
    KEY idx_cbr_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户自助注册绑定申请';
