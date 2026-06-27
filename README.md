# 蔬菜批发微信小程序

uni-app 前端 + Java Spring Boot 后端，替代纸单接单、分拣、录价、对账流程。

## 项目结构

```text
docs/                     产品、架构、部署、测试文档（见 docs/README.md）
backend/                  Java 后端（Spring Boot 3 + Sa-Token + Flyway）
  docker/                 MySQL + Redis + RabbitMQ（推荐本地基础设施）
  scripts/                联调 SQL、老板端 API 冒烟脚本
frontend/                 uni-app 前端（Vue 3 + TS + Pinia + uView Plus）
scripts/                  仓库级部署与冒烟（deploy-prod.sh、smoke-api.sh）
vegetable-wholesale-app/  已废弃（uni-app x 空壳，见 DEPRECATED.md）
```

## 本地环境要求

| 工具 | 用途 |
|------|------|
| JDK 21 | 后端 |
| Maven 3.9+ | 后端构建 |
| MySQL 8 | 业务数据库 |
| Redis | 缓存 / Sa-Token |
| RabbitMQ | 异步通知与统计刷新（可设 `MQ_ENABLED=false` 关闭） |
| Node.js 18+ | 前端 |
| 微信开发者工具 | 小程序预览 |
| Docker Desktop | 可选，一键起 MySQL/Redis/RabbitMQ |

## 快速开始

### 1. 基础设施

**推荐：Docker**

```bash
cd backend/docker && docker compose up -d
```

RabbitMQ 管理台：http://localhost:15672（`vwholesale` / `vwholesale`）

**或 Homebrew**

```bash
brew services start mysql redis rabbitmq
mysql -u root -e "CREATE DATABASE IF NOT EXISTS vwholesale; CREATE USER IF NOT EXISTS 'vwholesale'@'localhost' IDENTIFIED BY 'vwholesale'; GRANT ALL PRIVILEGES ON vwholesale.* TO 'vwholesale'@'localhost';"
```

若不用 RabbitMQ，启动后端前设置 `export MQ_ENABLED=false`。

### 2. 后端

```bash
cd backend
mvn spring-boot:run
```

- API 文档：http://localhost:8080/doc.html
- 健康检查：http://localhost:8080/api/health

可选：复制 `backend/src/main/resources/application-dev-local.example.yml` 为 `application-dev-local.yml`，填入微信 AppSecret（生成太阳码、真机微信登录）。该文件已 gitignore，勿提交。

JDK 路径问题可执行：`source backend/scripts/dev-env.sh`

### 3. 前端（微信小程序）

```bash
cd frontend
npm install --legacy-peer-deps
npm run dev:mp-weixin
```

微信开发者工具打开 **`frontend/dist/dev/mp-weixin`**。

### 4. H5 内测（浏览器）

```bash
cd frontend && npm run dev:h5
```

浏览器打开 http://localhost:5173。H5 无法微信登录，使用登录页 **Dev 快捷账号**（见下表）。

## Dev 测试账号（登录页一键切换）

| 角色 | openid | 登录页按钮 |
|------|--------|------------|
| 老板（管理员） | `dev-owner-001` | 老板登录 |
| 档口老板 | `dev-stall-owner-001` | 档口老板登录 |
| 档口经理 | `dev-stall-manager-001` | 档口经理登录 |
| 配送员 A / B | `dev-worker-001` / `002` | 配送员A / 配送员B |
| 客户 A / B / C | `dev-customer-001` / `002` / `003` | 客户A / B / C |

curl 示例：

```bash
curl -X POST http://localhost:8080/api/auth/dev-login \
  -H 'Content-Type: application/json' \
  -d '{"openid":"dev-owner-001","nickname":"老板","role":"OWNER_ADMIN"}'
```

## 核心业务流

**订单**（详见 [docs/order_status.md](docs/order_status.md)）：

```text
客户下单 → 待确认 → 老板确认 → 已确认（待拣单）
→ 拣单完成 → 已拣单 → 录价 → 已录价 → 对账 → 已对账 → 收款 → 已收款
```

**客户入驻**（详见 [docs/customer-onboarding.md](docs/customer-onboarding.md)）：

- **档口下单码**：我的 → 档口下单码，路人扫码临时下单
- **VIP 专属码**：客户管理 → 某客户，合作客户绑定档案
- **注册邀请**：客户管理 → 邀请注册，新客户填资料 + 老板审核

## 常用命令

| 用途 | 命令 |
|------|------|
| 后端 | `cd backend && mvn spring-boot:run` |
| 前端小程序 | `cd frontend && npm run dev:mp-weixin` |
| 前端 H5 | `cd frontend && npm run dev:h5` |
| API 冒烟 | `./scripts/smoke-api.sh`（仓库根目录，需后端已启动） |
| 老板 API 详细冒烟 | `cd backend && BASE_URL=http://localhost:8080 ./scripts/smoke-test-boss-apis.sh` |
| 生产部署 | `./scripts/deploy-prod.sh` |

## 三端页面概览

| 端 | 主要能力 |
|----|----------|
| **客户** | 商品下单、购物车、订单、VIP 绑定、注册申请 |
| **老板** | 订单工作台、拣单录价、商品/库存、客户/供应商、采购、收款对账、报表、人员、档口码 |
| **工人** | 待拣单、拣单详情、已拣单 |

## 文档

完整索引见 **[docs/README.md](docs/README.md)**。

| 文档 | 说明 |
|------|------|
| [docs/order_status.md](docs/order_status.md) | 订单六步状态（权威） |
| [docs/customer-onboarding.md](docs/customer-onboarding.md) | 三种客户码 |
| [docs/testing.md](docs/testing.md) | 本地测试脚本 |
| [docs/technical-architecture.md](docs/technical-architecture.md) | 技术架构 |
| [docs/vegetable-wholesale-miniapp-prd.md](docs/vegetable-wholesale-miniapp-prd.md) | **产品 PRD v1.0**（完整功能规格） |
| [docs/开发联调与生产部署指南.md](docs/开发联调与生产部署指南.md) | 日常 deploy + SSH 隧道 |
| [docs/腾讯云上线分步操作指南.md](docs/腾讯云上线分步操作指南.md) | 分步上云 |
| [backend/README.md](backend/README.md) | 后端专项说明 |
| [frontend/README.md](frontend/README.md) | 前端专项说明 |
