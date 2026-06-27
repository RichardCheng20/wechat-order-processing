# 蔬菜批发小程序技术架构设计

版本：v0.3  
日期：2026-06-12  
关联 PRD：**[vegetable-wholesale-miniapp-prd.md](./vegetable-wholesale-miniapp-prd.md) v1.0**  
开工清单：`docs/pre-development-checklist.md`

> **文档索引**：[docs/README.md](./README.md)  
> 下文 v0.3 为立项时设计；**§0 实现现状** 与 PRD v1.0 / 代码对齐（2026-06-27）。

---

## 0. 实现现状（与 v0.3 差异对照）

| 项 | v0.3 文档 | 当前代码 |
|----|-----------|----------|
| 前端目录 | `src/api`、`src/utils` | **`src/shared/api`、`src/shared/utils`**，别名 `@common` |
| API 路径 | `/api/products`、`/api/orders` 等扁平路径 | **`/api/boss/*`、`/api/customer/*`、`/api/worker/*`** |
| 后端包 | 独立 price/pick/report/ai 等 | 合并进 **order / product / payment**；无 `/api/ai` |
| MapStruct | 已确定 | **未引入**（`pom.xml` 无依赖） |
| 角色 | 合伙人管理员 | **`STALL_OWNER`（档口老板）、`STALL_MANAGER`（档口经理）** |
| 邀请码 | 默认 7 天 | **VIP 专属码绑定前长期有效**；注册邀请仍 7 天 |
| 库存 | PRD 暂不 MVP | **已实现**（Flyway + 库存页 + 扣减） |
| AI/OCR | 架构 §8 | **无后端 AI**；图片订单人工录入；文字解析在 `parseOrderText.ts` |
| 消息队列 | 未写进 v0.3 正文 | **RabbitMQ**（可 `MQ_ENABLED=false` 关闭） |
| 订单确认 | 部分文档写「确认已交货」 | **`PENDING_CONFIRM` → `PENDING_PICK`**，展示为「已确认」；见 [order_status.md](./order_status.md) |

**权威参考**：运行中 Knife4j（`/doc.html`）、[order_status.md](./order_status.md)、[customer-onboarding.md](./customer-onboarding.md)。

---

## 1. 技术决策结论

本项目采用“uni-app 前端 + Java 自建后端”的架构，不使用 uniCloud 承载核心业务。

选择原因：

- 项目核心是订单、客户、商品、价格、派单、对账，属于强业务系统。
- 老板擅长后端开发，Java 后端可以更好地承载复杂规则、权限、事务和审计。
- 同一个商品对不同客户价格不同，价格和金额必须在服务端统一计算和校验。
- 工人不能看到价格、订单金额、客户欠款和经营数据，权限需要服务端强控制。
- 后续可能扩展 Web 管理后台、AI 识别、微信支付、打印、ERP 对接，自建后端更可控。

### 1.1 已确认决策（2026-06-12）

| 类别 | 决策 |
|------|------|
| 前端框架 | 标准 **uni-app + Vue 3 + TypeScript**（废弃 uni-app x 空壳，重建 `frontend/`） |
| UI 组件 | **uView Plus** |
| 后端鉴权 | **Sa-Token** |
| 数据库迁移 | **Flyway** |
| 商户模型 | 首版运行时**单商户**（`merchant_id = 1`）；**保留多商户扩展**，数据层与接口均带 `merchant_id` |
| 小程序主体 | **暂定为个人**，后续升级个体工商户认证 |
| 主管理员初始化 | 首次微信登录 + **配置/白名单**自动升级 |
| 合伙人权限 | 首版**固定权限子集**，不做逐项可配置 |
| 客户下单确认 | 客户档案可配置 **自动确认**；未开启则进「待确认」 |
| 客户端价格 | **下单阶段不展示价格**；订单无金额直至老板送达后录价；录价完成后客户可见订单金额 |
| 邀请码有效期 | VIP 专属码 **绑定前长期有效**；注册邀请默认 **7 天**（见 [customer-onboarding.md](./customer-onboarding.md)） |
| 工人地址可见性 | 仅显示**客户简称 + 区域/门牌简写**，不显示电话和完整地址 |
| 项目目录 | `docs/`、`backend/`、`frontend/` |

## 2. 总体架构

```text
uni-app 前端
  -> 微信小程序
  -> 后续可发布 H5 / Android / iOS

Java Spring Boot 后端
  -> 登录鉴权
  -> 客户管理
  -> 商品管理
  -> 订单管理
  -> 派单与工人任务
  -> 录价与结算
  -> 报表统计
  -> AI 订单解析网关

基础设施
  -> MySQL
  -> Redis
  -> 对象存储 COS/OSS
  -> Nginx
  -> Docker
  -> 云服务器
```

## 3. 前端技术栈

已确定：

- uni-app（标准版，Vue 3 + TypeScript）
- Vue 3
- TypeScript
- Pinia
- **uView Plus**
- Vite
- 微信开发者工具
- HBuilderX（运行与预览）
- Cursor（辅助编码）

说明：不使用 uni-app x（`.uvue` / `.uts`）。现有 `vegetable-wholesale-app/` 废弃，按本文档重建 `frontend/`。

前端端内角色：

- 客户端：商品浏览、语音/手写/文字下单、订单状态。
- 老板端：订单工作台、客户管理、商品管理、派单、录价、日报。
- 工人端：只看被派发订单、客户货物清单、分拣/配送状态，不展示价格。

前端原则：

- 前端只做展示、交互和表单校验。
- 订单金额、客户价格、权限判断必须由后端处理。
- 大模型 API Key、OCR Key、语音识别 Key 不允许出现在前端代码中。

## 4. 后端技术栈

已确定：

- Java 21
- Spring Boot 3
- Spring Web
- Spring Validation
- **Sa-Token**（登录鉴权、角色权限）
- MyBatis-Plus
- MySQL 8
- Redis
- **Flyway**（数据库版本迁移）
- Knife4j / OpenAPI
- Lombok
- MapStruct
- Docker

可选：

- MinIO：本地开发对象存储替代。
- XXL-JOB：后续做定时统计、账单生成时再引入。
- WebSocket / SSE：后续做订单实时提醒时再引入。

## 5. 后端模块拆分

建议按业务模块拆包：

```text
auth         登录、微信 code 换 session、token、角色、管理员白名单
user         用户、主管理员、合伙人管理员、工人、客户绑定
customer     客户档案、邀请码、价格等级、账期、自动确认配置
product      商品、分类、别名、单位、上下架
price        今日基础价、客户特殊价、订单成交价
order        订单、订单明细、状态流转
dispatch     派单、改派、工人任务
pick         分拣、实际数量、缺货、换货
delivery     装车、送达、配送备注
payment      收款、应收、客户账单
report       日报、客户统计、商品出货统计
ai           OCR、语音识别、大模型订单解析
file         图片、语音、附件上传
common       异常、响应、权限注解、审计日志
config       商户配置、管理员白名单、邀请码默认有效期
```

### 5.1 商户模型（单商户运行，多商户预留）

首版只服务一家批发档口，但产品后续可能商业化并支持多个商户，因此从第一天起保留扩展能力：

**首版运行时（MVP）**

- 所有业务数据 `merchant_id = 1`。
- 不开发多商户 SaaS UI、租户注册、商户自助开通等能力。
- Flyway 初始化脚本预置一条默认商户记录。

**必须保留的扩展设计（为商业化做准备）**

- 所有核心业务表（客户、商品、订单、价格、工人、收款等）均包含 `merchant_id` 字段。
- 后端查询、写入、权限校验逻辑统一以 `merchant_id` 为租户边界（首版传入固定值 1）。
- 用户表保留 `merchant_id`，管理员/工人/客户均归属商户。
- API 层预留从登录态解析 `merchant_id` 的能力，首版可硬编码或从 token 读取固定值。
- 后续扩展多商户时，主要增量为：商户注册/开通、超管后台、按 `merchant_id` 隔离数据与配置，**无需重构表结构**。

```text
MVP 阶段：
  所有请求 -> merchant_id = 1（固定）

商业化阶段（后续）：
  登录态 -> 解析 merchant_id -> 全链路租户隔离
```

### 5.2 主管理员白名单

第一个主管理员通过配置自动升级，不依赖手动建账号：

```yaml
# application.yml 示例
app:
  admin:
    openid-whitelist:
      - oXXXX_main_admin_openid
    auto-upgrade-enabled: true
```

流程：

1. 用户微信登录，后端换取 openid。
2. 若 openid 在白名单中且尚未绑定管理员角色，自动创建/升级为主管理员（`OWNER_ADMIN`）。
3. 主管理员可在小程序内添加合伙人管理员和工人。

### 5.3 合伙人管理员固定权限（MVP）

首版不做权限逐项配置，合伙人管理员固定拥有以下能力：

| 能力 | 合伙人 |
|------|--------|
| 查看/处理订单 | ✅ |
| 派单 | ✅ |
| 录价 | ✅ |
| 查看日报/出货统计 | ✅ |
| 客户管理 | ✅ |
| 商品管理 | ✅ |
| 删除订单 | ❌ |
| 管理主管理员 | ❌ |
| 管理合伙人账号 | ❌ |
| 查看/修改采购价 | ❌ |
| 查看利润 | ❌ |

## 6. 核心数据表建议

### 6.1 users

用户微信身份表。

关键字段：

- id
- openid
- unionid
- nickname
- phone
- role：CUSTOMER、OWNER_ADMIN、PARTNER_ADMIN、WORKER
- customer_id
- merchant_id
- status：PENDING_BIND、ENABLED、DISABLED
- last_login_at
- created_at
- updated_at

### 6.2 customers

批发客户档案。

关键字段：

- id
- merchant_id（首版固定 1）
- name
- contact_name
- phone
- address（完整地址，仅老板/管理员可见）
- address_short（区域/门牌简写，工人端可见，如「城南农贸 3 号门」）
- default_delivery_time
- settlement_type：CASH、DAILY、WEEKLY、MONTHLY、CREDIT
- price_level
- auto_confirm_order：是否自动确认订单（true 则下单后直接进入待分拣）
- bind_user_id
- bind_status：NOT_INVITED、INVITED、BOUND、DISABLED
- invite_code
- invite_expired_at（默认生成后 7 天）
- remark
- status

### 6.3 workers

工人档案。

关键字段：

- id
- merchant_id
- user_id
- name
- phone
- status
- remark

### 6.4 products

商品表。

关键字段：

- id
- merchant_id
- category_id
- name
- aliases
- unit
- spec
- default_price
- default_purchase_price
- stock_qty
- safety_stock_qty
- sale_status：ON、OFF
- created_at
- updated_at

### 6.5 product_prices

商品价格表，用于今日基础价和客户特殊价。

关键字段：

- id
- merchant_id
- product_id
- customer_id：为空时表示基础价
- price
- effective_date
- status
- created_by
- created_at

价格优先级：

1. 订单明细成交价
2. 客户特殊价
3. 今日基础价
4. 商品默认价

### 6.6 orders

订单主表。

关键字段：

- id
- merchant_id
- order_no
- customer_id
- source：CUSTOMER_APP、BOSS_MANUAL、IMAGE、TEXT、VOICE、COPY_HISTORY
- status：DRAFT、PENDING_CONFIRM、PENDING_DISPATCH、PENDING_PICK、PICKING、PICKED、DELIVERING、DELIVERED、PENDING_PRICE、COMPLETED、CANCELLED
- delivery_date
- delivery_address（完整地址，工人接口不返回）
- delivery_address_short（简写地址，工人接口返回）
- contact_name
- contact_phone（工人接口不返回）
- assigned_worker_id
- amount
- paid_amount
- receivable_amount
- remark
- created_by
- created_at
- updated_at

### 6.7 order_items

订单明细表。

关键字段：

- id
- order_id
- product_id
- original_text
- order_qty
- actual_qty
- unit
- deal_price
- subtotal_amount
- shortage_flag
- substitute_product_id
- pick_remark
- created_at
- updated_at

注意：

- 工人接口不能返回 `deal_price` 和 `subtotal_amount`。
- 报表、账单、客户应收以服务端计算结果为准。

### 6.8 dispatch_logs

派单日志。

关键字段：

- id
- order_id
- from_worker_id
- to_worker_id
- action：ASSIGN、REASSIGN、UNASSIGN
- operator_user_id
- remark
- created_at

### 6.9 payments

收款记录。

关键字段：

- id
- merchant_id
- customer_id
- order_id
- amount
- method：CASH、WECHAT、BANK_TRANSFER、OTHER
- paid_at
- operator_user_id
- remark

## 7. 权限模型

### 7.1 客户

客户可访问：

- 自己可见的商品列表。
- 自己的订单。
- 自己的地址、联系人、账单。

客户不可访问：

- 其他客户信息。
- 工人任务。
- 老板端报表。
- 采购价、利润、其他客户价格。

### 7.2 主管理员和合伙人管理员

主管理员可访问：

- 全部客户、商品、订单、价格、派单、收款、报表。
- 合伙人管理员账号管理（添加/停用）。
- 为同一商品设置不同客户价格。
- 给订单派发工人。
- 录入最终成交价。
- 删除订单（需保留操作记录）。

合伙人管理员可访问（首版固定子集，不可配置）：

- 查看和处理订单、派单、录价、日报、出货统计。
- 客户管理和商品管理。
- 不能删除订单。
- 不能管理主管理员和合伙人账号。
- 不能查看采购价和利润。

### 7.3 工人

工人可访问：

- 被派发给自己的订单任务。
- 客户简称（`customers.name` 或简称字段）。
- 配送简写地址（`delivery_address_short`，如「城南农贸 3 号门」）。
- 商品名称、下单数量、实际分拣数量、单位、备注、缺货状态。
- 装车、送达状态。

工人不可访问：

- 商品单价。
- 订单金额。
- 客户欠款。
- 客户电话（`contact_phone`）。
- 完整配送地址（`delivery_address`）。
- 销售额。
- 利润。
- 采购价。
- 未派发给自己的订单。

工人接口响应需做字段级脱敏：不返回 `deal_price`、`subtotal_amount`、`contact_phone`、`delivery_address`、`amount` 等字段。

## 8. AI 接入设计

微信小程序、H5、Android、iOS 都不直接调用大模型。所有 AI 能力通过 Java 后端统一封装。

```text
前端上传图片/语音/文字
  -> Java 后端保存原始文件
  -> Java 后端调用 OCR / ASR / 大模型
  -> 返回结构化识别结果
  -> 前端展示确认页
  -> 用户确认后创建订单
```

MVP AI 能力优先级：

1. 文字下单解析：把“土豆 20 斤，西红柿 10 斤”解析为订单明细。
2. 语音转文字：先转文字，再走文字下单解析。
3. 图片 OCR：识别手写/截图中的文字，再走文字下单解析。
4. 大模型纠错：匹配商品别名、单位、数量，识别不确定项。

AI 结果原则：

- AI 只生成订单草稿，不直接生成正式订单。
- 识别结果必须经过客户或老板确认。
- 无法匹配商品时标记为“未识别商品”。
- 原始图片、语音、文本必须保留，便于追溯。

## 9. API 设计原则

接口前缀建议：

```text
/api/auth
/api/customer
/api/boss
/api/worker
/api/products
/api/orders
/api/dispatch
/api/prices
/api/payments
/api/reports
/api/ai
/api/files
```

关键接口示例：

```text
POST /api/auth/wechat-login
POST /api/customer/bind-by-invite
GET  /api/customer/products
POST /api/customer/orders
GET  /api/customer/orders

GET  /api/boss/orders
POST /api/boss/orders/manual
POST /api/boss/orders/{orderId}/confirm
POST /api/boss/orders/{orderId}/dispatch
POST /api/boss/orders/{orderId}/pricing
GET  /api/boss/reports/daily

GET  /api/worker/tasks
GET  /api/worker/tasks/{orderId}
POST /api/worker/tasks/{orderId}/start-pick
POST /api/worker/tasks/{orderId}/update-actual-qty
POST /api/worker/tasks/{orderId}/mark-picked
POST /api/worker/tasks/{orderId}/mark-delivered

POST /api/ai/parse-text-order
POST /api/ai/parse-voice-order
POST /api/ai/parse-image-order
```

## 10. 部署建议

开发环境：

- 本地 Java 服务
- 本地 MySQL
- 本地 Redis
- MinIO 或本地文件存储
- 微信开发者工具连接本地或测试域名

测试环境：

- 云服务器
- Docker Compose
- MySQL
- Redis
- Nginx
- 对象存储

生产环境：

- 云服务器或容器服务
- Nginx HTTPS
- MySQL 定期备份
- Redis 密码保护
- 对象存储私有桶
- 日志保留和错误告警

微信小程序上线要求：

- 后端接口必须配置 HTTPS 域名。
- 小程序后台需要配置 request 合法域名。
- 不把第三方 AI Key 放在小程序前端。

## 11. 开工顺序

建议按以下顺序推进，避免一上来就陷入 AI 和复杂报表。

### Step 1：项目骨架

- 初始化 uni-app 项目。
- 初始化 Spring Boot 项目。
- 配置 MySQL、Redis、统一响应、异常处理、接口文档。
- 跑通微信登录或临时登录。

### Step 2：基础主数据

- 用户和角色。
- 客户档案。
- 客户邀请绑定。
- 工人档案。
- 商品分类和商品库。

### Step 3：订单闭环

- 客户下单。
- 老板代客建单。
- 老板确认订单。
- 订单状态流转。
- 订单详情。

### Step 4：派单和工人任务

- 老板派单。
- 工人任务列表。
- 工人查看客户货物清单。
- 工人修改实际分拣数量。
- 工人标记缺货、拣完、送达。
- 权限验证：工人看不到价格。

### Step 5：录价和结算

- 今日基础价。
- 客户特殊价。
- 订单录价。
- 订单金额计算。
- 客户应收。

### Step 6：日报和出货统计

- 今日订单数、销售额、待分拣、待录价、商品出货统计。

### Step 7：AI 订单解析

- 文字订单解析。
- 语音转文字。
- 图片 OCR。
- 大模型辅助商品匹配。

## 12. 开发约束

- 所有金额计算必须在后端完成。
- 所有价格字段必须避免通过工人接口返回。
- 客户、主管理员、合伙人管理员、工人的接口必须做角色校验。
- 订单状态流转必须由后端校验，不允许前端直接改任意状态。
- 客户端商品列表/下单接口**不得返回**价格字段；客户下单时订单无单价与总额。
- 订单明细成交价必须落库，历史订单不受后续价格变化影响；**金额在老板录价后由服务端计算**。
- AI 识别结果必须走人工确认。
- 每次派单、改价、修改实际数量、删除订单都需要记录操作人和时间。
- 客户下单时：若 `auto_confirm_order = true` 则状态为 `PENDING_PICK`（待分拣），否则为 `PENDING_CONFIRM`（待确认）。
- 邀请码默认 7 天有效，过期后需老板重新生成。
- 首版运行时 `merchant_id` 固定为 1，不开发多商户 SaaS 功能；但所有表和接口必须带 `merchant_id`，为后续商业化扩展多商户做准备。

## 13. 项目目录

```text
02project_wechat_order_processing/
  docs/
  backend/
    src/main/java/
    src/main/resources/
    docker/
  frontend/
    src/
    pages/
    components/
    stores/
    api/
    utils/
    static/
```

说明：废弃现有 `vegetable-wholesale-app/`（uni-app x 空壳），按标准 uni-app 在 `frontend/` 重建。
