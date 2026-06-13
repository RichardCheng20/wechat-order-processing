-- 默认商户（MVP 固定 merchant_id = 1，保留多商户扩展）
CREATE TABLE merchants (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL COMMENT '商户名称',
    contact_name  VARCHAR(50)  NULL COMMENT '联系人',
    phone         VARCHAR(20)  NULL COMMENT '联系电话',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户表';

INSERT INTO merchants (id, name, status) VALUES (1, '默认蔬菜批发档口', 1);

-- 用户（微信身份 + 角色）
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '所属商户',
    openid        VARCHAR(64)  NOT NULL COMMENT '微信 openid',
    unionid       VARCHAR(64)  NULL COMMENT '微信 unionid',
    nickname      VARCHAR(100) NULL,
    phone         VARCHAR(20)  NULL,
    role          VARCHAR(32)  NOT NULL COMMENT 'CUSTOMER/OWNER_ADMIN/PARTNER_ADMIN/WORKER',
    customer_id   BIGINT       NULL COMMENT '客户角色关联客户档案',
    status        VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT 'PENDING_BIND/ENABLED/DISABLED',
    last_login_at DATETIME     NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_openid (openid),
    KEY idx_users_merchant_role (merchant_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 批发客户档案
CREATE TABLE customers (
    id                    BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id           BIGINT        NOT NULL DEFAULT 1,
    name                  VARCHAR(100)  NOT NULL COMMENT '客户名称',
    contact_name          VARCHAR(50)   NULL,
    phone                 VARCHAR(20)   NULL,
    address               VARCHAR(500)  NULL COMMENT '完整地址，管理员可见',
    address_short         VARCHAR(200)  NULL COMMENT '简写地址，工人可见',
    default_delivery_time VARCHAR(50)   NULL,
    settlement_type       VARCHAR(20)   NOT NULL DEFAULT 'CASH' COMMENT 'CASH/DAILY/WEEKLY/MONTHLY/CREDIT',
    price_level           VARCHAR(50)   NULL,
    auto_confirm_order    TINYINT       NOT NULL DEFAULT 0 COMMENT '1自动确认 0需老板确认',
    bind_user_id          BIGINT        NULL,
    bind_status           VARCHAR(20)   NOT NULL DEFAULT 'NOT_INVITED' COMMENT 'NOT_INVITED/INVITED/BOUND/DISABLED',
    invite_code           VARCHAR(32)   NULL,
    invite_expired_at     DATETIME      NULL,
    remark                VARCHAR(500)  NULL,
    status                TINYINT       NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    deleted               TINYINT       NOT NULL DEFAULT 0,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_customers_merchant (merchant_id),
    KEY idx_customers_invite_code (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户档案';

-- 工人档案
CREATE TABLE workers (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT       NOT NULL DEFAULT 1,
    user_id     BIGINT       NULL COMMENT '绑定的微信用户',
    name        VARCHAR(50)  NOT NULL,
    phone       VARCHAR(20)  NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    remark      VARCHAR(500) NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_workers_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工人档案';

-- 商品分类
CREATE TABLE product_categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT       NOT NULL DEFAULT 1,
    name        VARCHAR(50)  NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_categories_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

-- 商品
CREATE TABLE products (
    id                     BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id            BIGINT         NOT NULL DEFAULT 1,
    category_id            BIGINT         NULL,
    name                   VARCHAR(100)   NOT NULL,
    aliases                VARCHAR(500)   NULL COMMENT '别名，逗号分隔',
    unit                   VARCHAR(20)    NOT NULL DEFAULT '斤',
    spec                   VARCHAR(100)   NULL,
    default_price          DECIMAL(10, 2) NULL COMMENT '默认售价',
    default_purchase_price DECIMAL(10, 2) NULL COMMENT '默认采购价',
    sale_status            VARCHAR(10)    NOT NULL DEFAULT 'ON' COMMENT 'ON/OFF',
    deleted                TINYINT        NOT NULL DEFAULT 0,
    created_at             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_products_merchant (merchant_id),
    KEY idx_products_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品';

-- 商品价格（今日基础价 + 客户特殊价）
CREATE TABLE product_prices (
    id             BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id    BIGINT         NOT NULL DEFAULT 1,
    product_id     BIGINT         NOT NULL,
    customer_id    BIGINT         NULL COMMENT '为空表示基础价',
    price          DECIMAL(10, 2) NOT NULL,
    effective_date DATE           NOT NULL,
    status         TINYINT        NOT NULL DEFAULT 1,
    created_by     BIGINT         NULL,
    deleted        TINYINT        NOT NULL DEFAULT 0,
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_prices_product_date (product_id, effective_date),
    KEY idx_prices_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品价格';

-- 订单主表
CREATE TABLE orders (
    id                      BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id             BIGINT         NOT NULL DEFAULT 1,
    order_no                VARCHAR(32)    NOT NULL COMMENT '订单编号',
    customer_id             BIGINT         NOT NULL,
    source                  VARCHAR(32)    NOT NULL COMMENT 'CUSTOMER_APP/BOSS_MANUAL/IMAGE/TEXT/VOICE/COPY_HISTORY',
    status                  VARCHAR(32)    NOT NULL COMMENT 'DRAFT/PENDING_CONFIRM/PENDING_PICK/PICKING/PICKED/DELIVERING/DELIVERED/PENDING_PRICE/COMPLETED/CANCELLED',
    delivery_date           DATE           NULL,
    delivery_address        VARCHAR(500)   NULL COMMENT '完整地址',
    delivery_address_short  VARCHAR(200)   NULL COMMENT '简写地址',
    contact_name            VARCHAR(50)    NULL,
    contact_phone           VARCHAR(20)    NULL,
    assigned_worker_id      BIGINT         NULL,
    amount                  DECIMAL(12, 2) NULL COMMENT '订单总额',
    paid_amount             DECIMAL(12, 2) NULL DEFAULT 0,
    receivable_amount       DECIMAL(12, 2) NULL,
    remark                  VARCHAR(500)   NULL,
    created_by              BIGINT         NULL,
    deleted                 TINYINT        NOT NULL DEFAULT 0,
    created_at              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_orders_order_no (order_no),
    KEY idx_orders_merchant_status (merchant_id, status),
    KEY idx_orders_customer (customer_id),
    KEY idx_orders_delivery_date (delivery_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

-- 订单明细
CREATE TABLE order_items (
    id                    BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id              BIGINT         NOT NULL,
    product_id            BIGINT         NULL,
    original_text         VARCHAR(200)   NULL COMMENT '原始识别文本',
    order_qty             DECIMAL(10, 2) NOT NULL DEFAULT 0,
    actual_qty            DECIMAL(10, 2) NULL COMMENT '实际分拣数量',
    unit                  VARCHAR(20)    NOT NULL DEFAULT '斤',
    deal_price            DECIMAL(10, 2) NULL COMMENT '成交单价',
    subtotal_amount       DECIMAL(12, 2) NULL COMMENT '小计',
    shortage_flag         TINYINT        NOT NULL DEFAULT 0,
    substitute_product_id BIGINT         NULL,
    pick_remark           VARCHAR(200)   NULL,
    deleted               TINYINT        NOT NULL DEFAULT 0,
    created_at            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_order_items_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

-- 派单日志
CREATE TABLE dispatch_logs (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id         BIGINT       NOT NULL,
    from_worker_id   BIGINT       NULL,
    to_worker_id     BIGINT       NULL,
    action           VARCHAR(20)  NOT NULL COMMENT 'ASSIGN/REASSIGN/UNASSIGN',
    operator_user_id BIGINT       NULL,
    remark           VARCHAR(200) NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_dispatch_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='派单日志';

-- 收款记录
CREATE TABLE payments (
    id               BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id      BIGINT         NOT NULL DEFAULT 1,
    customer_id      BIGINT         NOT NULL,
    order_id         BIGINT         NULL,
    amount           DECIMAL(12, 2) NOT NULL,
    method           VARCHAR(20)    NOT NULL COMMENT 'CASH/WECHAT/BANK_TRANSFER/OTHER',
    paid_at          DATETIME       NOT NULL,
    operator_user_id BIGINT         NULL,
    remark           VARCHAR(200)   NULL,
    deleted          TINYINT        NOT NULL DEFAULT 0,
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_payments_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收款记录';

-- 操作审计日志
CREATE TABLE audit_logs (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id   BIGINT       NOT NULL DEFAULT 1,
    user_id       BIGINT       NULL,
    action        VARCHAR(50)  NOT NULL,
    target_type   VARCHAR(50)  NULL,
    target_id     BIGINT       NULL,
    detail        TEXT         NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audit_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志';

-- 初始化商品分类
INSERT INTO product_categories (merchant_id, name, sort_order) VALUES
(1, '叶菜', 1),
(1, '根茎', 2),
(1, '瓜果', 3),
(1, '菌菇', 4),
(1, '葱姜蒜', 5),
(1, '豆制品', 6),
(1, '其他', 99);
