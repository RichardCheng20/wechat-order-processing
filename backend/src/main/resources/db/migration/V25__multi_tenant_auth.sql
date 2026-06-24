-- 多租户登录：用户按商户+openid 唯一；商户开通码；员工激活码

ALTER TABLE merchants
    ADD COLUMN onboarding_token VARCHAR(32) NULL COMMENT '档口开通码，首个微信扫码认领主管理员' AFTER status,
    ADD COLUMN owner_user_id BIGINT NULL COMMENT '主管理员用户ID' AFTER onboarding_token;

UPDATE merchants SET onboarding_token = 'OPEN0001' WHERE id = 1 AND onboarding_token IS NULL;

UPDATE merchants m
SET owner_user_id = (
    SELECT u.id FROM users u
    WHERE u.merchant_id = m.id AND u.role = 'OWNER_ADMIN' AND u.deleted = 0
    ORDER BY u.id LIMIT 1
)
WHERE m.id = 1 AND m.owner_user_id IS NULL;

ALTER TABLE users DROP INDEX uk_users_openid;
ALTER TABLE users ADD UNIQUE KEY uk_users_merchant_openid (merchant_id, openid);

CREATE TABLE staff_activation_tokens (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id     BIGINT       NOT NULL,
    token           VARCHAR(32)  NOT NULL COMMENT '激活码',
    target_role     VARCHAR(32)  NOT NULL COMMENT 'WORKER/PARTNER_ADMIN',
    worker_id       BIGINT       NULL COMMENT '关联人员档案',
    expired_at      DATETIME     NOT NULL,
    used_at         DATETIME     NULL,
    used_by_user_id BIGINT       NULL,
    created_by      BIGINT       NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_staff_activation_token (token),
    KEY idx_staff_activation_merchant (merchant_id),
    KEY idx_staff_activation_worker (worker_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工/合伙人微信激活码';
